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
      timeoutSecs = 10L,
      verbose = true
    )
    println("-- Running benchmark ...")
    val (_, _, _, times3) = loopStatisticsWithTimeout(
      () => SubmissionPublisherLoops3Test(ITEMS).main(),
      BENCHMARK_RUNS,
      timeoutSecs = 10L,
      verbose = false
    )
    println("-- Statistics:")
    println(f" Average time: ${average(times3)}%7.3f seconds")
    println(f" Std Dev time: ${stddev(times3)}%7.3f seconds")
    println(s"-- End of SubmissionPublisherLoops3Test session --")
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
            f"Iteration ${i + 1}: Success, per step time: ${timeSeconds}%7.3f seconds"
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
      f"Loop completed: ${successCount} successes (${successCount.toDouble / count}%3.4f), ${timeoutCount} timeouts (${timeoutCount.toDouble / count}%3.4f), ${failureCount} failures, total ${count} iterations."
    )
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
