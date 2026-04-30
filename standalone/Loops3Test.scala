import java.util.concurrent._

object Test {

  def main(args: Array[String]): Unit = {
    println("Hello, World!")

    val WARMUP_RUNS = 10
    val BENCHMARK_RUNS = 100
    val NPS: Long = 1000L * 1000 * 1000
    val ITEMS = 1 << 20

    println(s"-- Benchmark for SubmissionPublisherLoops3Test --")
    println("-- Warming up ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops3Test(ITEMS).main(),
      WARMUP_RUNS,
      timeoutSecs = 60L,
      verbose = true,
      printStats = false
    )
    println("-- Running benchmark ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops3Test(ITEMS).main(),
      BENCHMARK_RUNS,
      timeoutSecs = 60L,
      verbose = true,
      printStats = true
    )
    println(s"-- End of SubmissionPublisherLoops3Test session --")
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

/** Creates PRODUCERS publishers each with CONSUMERS subscribers, each sent
  * ITEMS items, with CAP buffering; repeats REPS times
  */
class SubmissionPublisherLoops3Test(items: Int) {

  val ITEMS: Int = items
  /* Original JSR-166 parameters: */
  val PRODUCERS = 32
  val CONSUMERS = 32

  val CAP: Int = Flow.defaultBufferSize()
  val phaser = new Phaser(PRODUCERS * CONSUMERS + 1)

  def main(): Unit = {
    // System.out.println(
    //   "ITEMS: " + ITEMS + " PRODUCERS: " + PRODUCERS + " CONSUMERS: " + CONSUMERS +
    //     " CAP: " + CAP
    // )
    oneRun()
  }

  def oneRun(): Unit = {
    val nitems = ITEMS.toLong * PRODUCERS * CONSUMERS
    val startTime = System.nanoTime()
    for (i <- 0 until PRODUCERS) {
      new Pub().fork()
    }
    phaser.arriveAndAwaitAdvance()
    val elapsed = System.nanoTime() - startTime
    val secs = elapsed.toDouble / (1000L * 1000 * 1000)
    val ips = nitems / secs
    // System.out.println(f"  items per sec: ${ips}%14.2f")
  }

  final class Sub extends Flow.Subscriber[Boolean] {
    var count = 0
    var subscription: Flow.Subscription = null

    def onSubscribe(s: Flow.Subscription): Unit = {
      subscription = s
      s.request(CAP)
    }

    def onNext(b: Boolean): Unit = {
      if (b && ({ count += 1; count } & ((CAP >>> 1) - 1)) == 0)
        subscription.request(CAP >>> 1)
    }

    def onComplete(): Unit = {
      if (count != ITEMS)
        System.err.println("Error: remaining " + (ITEMS - count))
      phaser.arrive()
    }

    def onError(t: Throwable): Unit = {
      t.printStackTrace()
    }
  }

  final class Pub extends RecursiveAction {
    final val pub =
      new SubmissionPublisher[Boolean](ForkJoinPool.commonPool(), CAP)

    def compute(): Unit = {
      val p = pub
      for (i <- 0 until CONSUMERS) {
        p.subscribe(new Sub())
      }
      for (i <- 0 until ITEMS) {
        p.submit(true)
      }
      p.close()
    }
  }
}
