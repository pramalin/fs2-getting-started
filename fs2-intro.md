---
title: Introduction to FS2
author: Padhu Ramalingam
theme: beige
highlightTheme: atom-one-light
revealOptions:
  transition: slide
---


### fs2 - Functional Streams for Scala


##### History

https://fs2.io/
 - v1.0.0  Oct  5, 2018

 ****Was scalaz-stream***
 - v0.8.5 Oct 23, 2016 
 - v0.1   Oct 10, 2013 

---

### About me

![](github.png)

Note:
- Senior developer for Citi
- Facinated by FP 

---

## Dependencies
  - <!-- .element: class="fragment" data-fragment-index="1"--> Cats Effect 
> <!-- .element: class="fragment" data-fragment-index="1" -->The IO Monad for Scala
  - <!-- .element: class="fragment" data-fragment-index="2" --> Cats
> <!-- .element: class="fragment" data-fragment-index="2" -->Lightweight, modular, and extensible library for functional programming

---

### REPL setup
Download [ammonite](http://ammonite.io/#Ammonite-REPL)
```sh
>java -jar ~/tools/ammonite/amm-2.12-1.6.2.jar

Welcome to the Ammonite Repl 1.6.2
(Scala 2.12.8 Java 1.8.0_181)

@ interp.load.ivy("co.fs2" %% "fs2-core" % "1.0.2")
```
---

## Pure

**Referential transparency**:
> Where one can replace an expression for its value, without changing the result

Note:

----

## Side Effects

```scala
val expr = 123
// expr: Int = 123

(expr, expr)
// res1: (Int, Int) = (123, 123)

val expr = println("Hey!")
// Hey!

(expr, expr)
// res3: (Unit, Unit) = ((), ())
```
<!-- .element: class="fragment" --> **Not pure**
----

## Managing effects
#### using IO

```scala 
import cats.effect.IO

IO(println("Hey!"))
// res13: IO[Unit] = Delay(...)
res13.unsafeRunSync
// Hey!

``` 

<!-- .element: class="fragment" --> **Pure**

----

## Managing effects
#### IO encapsulates the effects to maintain RT

```scala
def putStrLn(line: String): IO[Unit] =  IO {println(line)}
def f(a: IO[Unit], b: IO[Unit]): Unit = { 
  a.unsafeRunSync; b.unsafeRunSync
}

f(putStrLn("hi!"), putStrLn("hi!"))
// hi!
// hi!

val x = putStrLn("hi!")
f(x, x)
// hi!
// hi!
```
<!-- .element: class="fragment" -->


----

## Side effects in concurrency

```scala
import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.syntax.apply._   // for mapN
import cats.instances.all._  // implicits

val read : Future[Int] = Future(StdIn.readInt)
// does not work as Future is eager
val result : Future[Int] = (read, read).mapN(_ + _)
```

<!-- .element: class="fragment" --> **Not pure**

----

## Stream
##### (scala.collection)
```scala
Stream(1, 2, 3)
// res1: Stream[Int] = Stream(1, 2, 3)

Stream(1, 2, 3).map(_ + 1)
// res2: Stream[Int] = Stream(2, 3, 4)

Stream(1, 2, 3).map(_ + 1).map(_.toString)
// res3: Stream[String] = Stream("2", "3", "4")
```
Notes:
- Refresher collection Stream before IO Streams

----
## FS2 Stream
```scala
import fs2._

Stream(1, 2, 3)
// res2: Stream[Nothing, Int] = Stream(..)

Stream(1, 2, 3).map(_ + 1)
// res3: Stream[Nothing, Int] = Stream(..)

Stream(1, 2, 3).map(_ + 1).map(_.toString)
// res4: Stream[Nothing, String] = Stream(..)

Stream(1, 2, 3).flatMap { n => Stream.emit(List.fill(n)(n))}
// res5: Stream[Nothing, List[Int]] = Stream(..)
```
---
## Pure Stream
***Still no IO***
```scala
val s = Stream(1, 2, 3)
// s: Stream[Nothing, Int] = Stream(..)

val t = Stream(4, 5, 6)
// t: Stream[Nothing, Int] = Stream(..)

val u = s interleave t
// u: Stream[Nothing, Int] = Stream(..)

u.toList
// res9: List[Int] = List(1, 4, 2, 5, 3, 6)

val s2 = Stream.range(0, 10)
// s2: Stream[Nothing, Int] = Stream(..)

val t2 = s2.intersperse(-42)
// t2: Stream[Nothing, Int] = Stream(..)

t2.toList
// res12: List[Int] = List(0, -42, 1, -42, 2, -42, 3, -42, 4, -42, 5, -42, 6, -42, 7, -42, 8, -42, 9)

val s3 = Stream.range(0, 10)
// s3: Stream[Nothing, Int] = Stream(..)

val t3 = Stream(1, 2, 3)
// t3: Stream[Nothing, Int] = Stream(..)

s3.zip(t3).toList
// res15: List[(Int, Int)] = List((0, 1), (1, 2), (2, 3))
```
---
## IO Monad 

```scala

 // trait Monad[F[_]] extends Functor[F] {
  def pure[A](a: => A): F[A] = ???
  def flatMap[A,B](ma: F[A])(f: A => F[B]): F[B] = ???

import cats.effect.IO

val t = IO.pure(42)
// t: IO[Int] = Pure(42)

t.map {i => i + 1}
// res4: IO[Int] = Map(Pure(42), ...)

res4.unsafeRunSync
// res5: Int = 43


```
Note:
- pure and flatmap are the primitive combinators.
- map is a derived combinator.
- IO Monad have other methods to support concurrency, cancel, etc.

---
## IO Monad - concurrency 

```scala
import cats.effect.ContextShift
implicit val ioContextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)

val t = IO { println(s"Computing on ${Thread.currentThread.getName}..."); System.currentTimeMillis}
t.unsafeRunSync
// Computing on main...
// res12: Long = 1548213244359L

val t = IO.delay { println(s"Computing on ${Thread.currentThread.getName}..."); System.currentTimeMillis}
t.unsafeRunSync
// Computing on main...
// res14: Long = 1548213268629L

t.start.unsafeRunSync
//Computing on scala-execution-context-global-55...
//res15: cats.effect.Fiber[IO, Long] = Tuple(
//  Bind(Pure(Right(1548213499660L)), cats.MonadError$$Lambda$1813/32485743@1a10bf),
//  Suspend(cats.effect.internals.IOConnection$Impl$$Lambda$1808/14557395@1ffa400))
```

Notes: 
- ContextShift implicit is essntial for concurrency
- Displaying the thread name to validate
- one example 'start' is shown here.

---
# Questions?

- ??? 

