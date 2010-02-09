package com.twitter.gizzard

import java.util.concurrent._
import com.twitter.xrayspecs.Duration


class Future(name: String, poolSize: Int, maxPoolSize: Int, keepAlive: Duration) {
  private val executor = new ThreadPoolExecutor(poolSize, maxPoolSize, keepAlive.inSeconds, TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable], new NamedPoolThreadFactory(name))

  def apply[A](a: => A) = {
    val future = new FutureTask(new Callable[A] {
      def call = a
    })
    executor.execute(future)
    future
  }
}

class ParallelSeq[A](seq: Seq[A], future: Future) extends Seq[A] {
  def length = seq.length

  def apply(i: Int) = seq(i)

  def elements = seq.elements

  override def map[B](f: A => B) = {
    seq.map { a => future(f(a)) }.map(_.get(6000, TimeUnit.MILLISECONDS))
  }

  override def flatMap[B](f: A => Iterable[B]) = {
    seq.map { a => future(f(a)) }.flatMap(_.get(6000, TimeUnit.MILLISECONDS))
  }
}
