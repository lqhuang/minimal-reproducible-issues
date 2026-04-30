import java.util.concurrent._

object Test {

  def main(args: Array[String]): Unit = {
    println("Hello, World!")

    val WARMUP_RUNS = 10
    val BENCHMARK_RUNS = 100
    val NPS: Long = 1000L * 1000 * 1000
    val ITEMS = 1 << 20

    println(s"-- Benchmark for SubmissionPublisherLoops1Test --")
    println("-- Warming up ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops1Test(ITEMS).main(),
      WARMUP_RUNS,
      timeoutSecs = 10L,
      verbose = true,
      printStats = false
    )
    println("-- Running benchmark ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops1Test(ITEMS).main(),
      BENCHMARK_RUNS,
      timeoutSecs = 10L,
      verbose = true,
      printStats = true
    )
    println(s"-- End of SubmissionPublisherLoops1Test session --")
  }

  def average(xs: List[Double]): Double =
    xs.sum / xs.size

  def stddev(xs: List[Double]): Double = {
    val mean = average(xs)
    math.sqrt(
      xs.map(x => math.pow(x - mean, 2)).sum / (xs.size - 1)
    )
  }

  @inline
  def loopStatisticsWithTimeout(
      func: () => Unit,
      count: Int,
      timeoutSecs: Long,
      verbose: Boolean,
      printStats: Boolean
  ): (Int, Int, Int, List[Double]) = {
    var successCount = 0
    var timeoutCount = 0
    var failureCount = 0
    var times = List[Double]()

    for (i <- 0 until count) {
      try {
        val tic = System.nanoTime()
        val task = CompletableFuture
          .runAsync(() => { func() })
          .orTimeout(timeoutSecs, TimeUnit.SECONDS)
          .join()
        val toc = System.nanoTime()
        val timeSeconds = (toc - tic).toDouble / (1000L * 1000 * 1000)
        times = times.appended(timeSeconds)
        if (verbose || (i % 10 == 0))
          println(
            f"Iteration ${i + 1}%04d: Success, per iter time: ${timeSeconds}%6.4f seconds"
          )
        successCount += 1
      } catch {
        case ex: CompletionException => {
          val cause = ex.getCause()
          if (cause.isInstanceOf[TimeoutException]) {
            timeoutCount += 1
            if (verbose || (i % 10 == 0))
              println(f"Iteration ${i + 1}%04d: Timeout")
          } else {
            failureCount += 1
            System.err.println(
              f"Iteration ${i + 1}%04d: Failure, exception: ${cause}"
            )
            ex.printStackTrace(System.err)
          }
        }
      } finally {
        Thread.sleep(500L)
      }
    }

    if (printStats) {
      println("-- Statistics:")
      println(f" Average time: ${average(times)}%6.4f seconds")
      println(f" Std Dev time: ${stddev(times)}%6.4f seconds")
      println(f"        Total: ${count}%5d runs")
      println(f"    Successes: ${successCount}%5d runs")
      println(f"    Succ Rate: ${successCount.toDouble / count}%.6f")
      println(f"     Timeouts: ${timeoutCount}%5d runs")
      println(f" Timeout Rate: ${timeoutCount.toDouble / count}%.6f")
      println(f"     Failures: ${failureCount}%5d runs")
      println(f" Failure Rate: ${failureCount.toDouble / count}%.6f")
    }

    (successCount, timeoutCount, failureCount, times)
  }

}

/** One publisher, many subscribers */
class SubmissionPublisherLoops1Test(items: Int) {

  val ITEMS: Int = items
  /* Parameters for multiple threading (original JSR-166 setup) */
  val CONSUMERS = 64

  val CAP: Int = Flow.defaultBufferSize()
  val phaser = new Phaser(CONSUMERS + 1)

  final class Sub extends Flow.Subscriber[Boolean] {
    var sn: Flow.Subscription = null
    var count = 0

    override def onSubscribe(s: Flow.Subscription): Unit = {
      sn = s
      s.request(CAP)
    }

    override def onNext(t: Boolean): Unit = {
      if (({ count += 1; count } & (CAP - 1)) == (CAP >>> 1))
        sn.request(CAP)
    }

    override def onError(t: Throwable): Unit = {
      t.printStackTrace()
    }

    override def onComplete(): Unit = {
      if (count != ITEMS)
        System.err.println("Error: remaining " + (ITEMS - count))
      phaser.arrive()
    }
  }

  def main(): Unit = {
    // System.out.println(
    //   "ITEMS: " + ITEMS + " CONSUMERS: " + CONSUMERS + " CAP: " + CAP
    // )
    val exec = ForkJoinPool.commonPool()
    oneRun(exec)
    if (exec ne ForkJoinPool.commonPool()) exec.shutdown()
  }

  def oneRun(exec: ExecutorService): Unit = {
    val pub = new SubmissionPublisher[Boolean](exec, CAP)
    for (i <- 0 until CONSUMERS) {
      pub.subscribe(new Sub())
    }
    for (i <- 0 until ITEMS) {
      pub.submit(true)
    }
    pub.close()
    phaser.arriveAndAwaitAdvance()
  }
}
