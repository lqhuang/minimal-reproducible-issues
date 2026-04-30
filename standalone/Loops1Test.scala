import java.util.concurrent._

object Test {

  def main(args: Array[String]): Unit = {
    println("Hello, World!")

    val WARMUP_RUNS = 5
    val BENCHMARK_RUNS = 20
    val NPS: Long = 1000L * 1000 * 1000
    val ITEMS = 1 << 20

    println(s"-- Benchmark for SubmissionPublisherLoops1Test --")
    println("-- Warming up ...")
    for (j <- 0 until WARMUP_RUNS) {
      print(f"Run warmup step ${j + 1} ...  ")
      val tic = System.nanoTime()
      SubmissionPublisherLoops1Test(ITEMS).main()
      val toc = System.nanoTime()
      val timeSeconds = (toc - tic).toDouble / NPS
      println(f"  per step time: ${timeSeconds}%7.3f seconds")
      Thread.sleep(1000L)
    }
    var times1: List[Double] = List()
    println("-- Running benchmark ...")
    for (j <- 0 until BENCHMARK_RUNS) {
      println(f"Run benchmark step ${j + 1} ...")
      try {
        val tic = System.nanoTime()
        CompletableFuture
          .runAsync(() => {
            SubmissionPublisherLoops1Test(ITEMS).main()
          })
          .orTimeout(10L, TimeUnit.SECONDS)
          .join()
        val toc = System.nanoTime()
        val timeSeconds = (toc - tic).toDouble / NPS
        times1 = times1.appended(timeSeconds)
        println(f"  per step time: ${timeSeconds}%7.3f seconds")
      } catch {
        case ex: CompletionException => {
          val cause = ex.getCause()
          if (cause.isInstanceOf[TimeoutException]) {
            println(f"  Step ${j + 1} timed out.")
          } else throw ex
        }
      }

      Thread.sleep(1000L)
    }
    println("-- Statistics:")
    println(f" Average time: ${average(times1)}%7.3f seconds")
    println(f" Std Dev time: ${stddev(times1)}%7.3f seconds")
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
      verbose: Boolean
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
        if (verbose)
          println(
            f"Iteration ${i + 1}: Success, per iter time: ${timeSeconds}%7.3f seconds"
          )
        successCount += 1
      } catch {
        case ex: CompletionException => {
          val cause = ex.getCause()
          if (cause.isInstanceOf[TimeoutException]) {
            timeoutCount += 1
            if (verbose) println(f"Iteration ${i + 1}: Timeout")
          } else {
            failureCount += 1
            System.err.println(
              f"Iteration ${i + 1}: Failure, exception: ${cause}"
            )
          }
        }
      } finally {
        Thread.sleep(500L)
      }
    }

    println(
      s"Loop completed: ${successCount} successes, ${timeoutCount} timeouts, ${failureCount} failures, total ${count} iterations."
    )
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
