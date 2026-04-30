import java.util.concurrent._

object Test {

  def main(args: Array[String]): Unit = {
    println("Hello, World!")

    val WARMUP_RUNS = 10
    val BENCHMARK_RUNS = 100
    val NPS: Long = 1000L * 1000 * 1000
    val ITEMS = 1 << 20

    println(s"-- Benchmark for SubmissionPublisherLoops4Test --")
    println("-- Warming up ...")
    loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops4Test(ITEMS).main(),
      WARMUP_RUNS,
      timeoutSecs = 10L,
      verbose = true
    )
    println("-- Running benchmark ...")
    val (successCount, timeoutCount, failureCount, times) =
      loopStatisticsWithTimeout(
        () => SubmissionPublisherLoops4Test(ITEMS).main(),
        BENCHMARK_RUNS,
        timeoutSecs = 10L,
        verbose = true
      )
    println("-- Statistics:")
    println(f" Average time: ${average(times)}%6.4f seconds")
    println(f" Std Dev time: ${stddev(times)}%6.4f seconds")
    println(f"        Total: ${BENCHMARK_RUNS}%5d runs")
    println(f"    Successes: ${successCount}%5d runs")
    println(f"    Succ Rate: ${successCount.toDouble / BENCHMARK_RUNS}%.6f")
    println(f"     Timeouts: ${timeoutCount}%5d runs")
    println(f" Timeout Rate: ${timeoutCount.toDouble / BENCHMARK_RUNS}%.6f")
    println(f"     Failures: ${failureCount}%5d runs")
    println(f" Failure Rate: ${failureCount.toDouble / BENCHMARK_RUNS}%.6f")
    println(s"-- End of SubmissionPublisherLoops4Test session --")
    println("")
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

    (successCount, timeoutCount, failureCount, times)
  }

}

/** Creates PRODUCERS publishers each with PROCESSORS processors each with
  * CONSUMERS subscribers, each sent ITEMS items, with max CAP buffering;
  * repeats REPS times
  */
class SubmissionPublisherLoops4Test(items: Int) {

  val ITEMS: Int = items
  /* Original JSR-166 parameters: */
  val PRODUCERS = 32
  val PROCESSORS = 32
  val CONSUMERS = 32

  val CAP: Int = Flow.defaultBufferSize()
  val SINKS: Int = PRODUCERS * PROCESSORS * CONSUMERS
  val phaser = new Phaser(SINKS + 1)

  def main(): Unit = {
    // System.out.println(
    //   "ITEMS: " + ITEMS +
    //     " PRODUCERS: " + PRODUCERS +
    //     " PROCESSORS: " + PROCESSORS +
    //     " CONSUMERS: " + CONSUMERS +
    //     " CAP: " + CAP
    // )
    oneRun()
  }

  def oneRun(): Unit = {
    val total: Long = ITEMS.toLong * SINKS
    val startTime = System.nanoTime()
    for (i <- 0 until PRODUCERS) {
      new Pub().fork()
    }
    phaser.arriveAndAwaitAdvance()
    val elapsed = System.nanoTime() - startTime
    val secs = elapsed.toDouble / (1000L * 1000 * 1000)
    val ips = total / secs
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

  final class Proc(executor: Executor, maxBufferCapacity: Int)
      extends SubmissionPublisher[Boolean](executor, maxBufferCapacity)
      with Flow.Processor[Boolean, Boolean] {
    var subscription: Flow.Subscription = null
    var count = 0

    def onSubscribe(subscription: Flow.Subscription): Unit = {
      this.subscription = subscription
      subscription.request(CAP)
    }

    def onNext(item: Boolean): Unit = {
      if (({ count += 1; count } & ((CAP >>> 1) - 1)) == 0)
        subscription.request(CAP >>> 1)

      submit(item)
    }

    def onError(ex: Throwable): Unit = {
      closeExceptionally(ex)
    }

    def onComplete(): Unit = {
      close()
    }
  }

  final class Pub extends RecursiveAction {
    final val pub =
      new SubmissionPublisher[Boolean](ForkJoinPool.commonPool(), CAP)

    def compute(): Unit = {
      val p = pub
      for (j <- 0 until PROCESSORS) {
        val t = new Proc(ForkJoinPool.commonPool(), CAP)
        for (i <- 0 until CONSUMERS) {
          t.subscribe(new Sub())
        }
        p.subscribe(t)
      }
      for (i <- 0 until ITEMS) {
        p.submit(true)
      }
      p.close()
    }
  }
}
