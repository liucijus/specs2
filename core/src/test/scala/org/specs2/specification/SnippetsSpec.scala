package org.specs2
package specification

import matcher.DataTables
import execute.Snippet._

class SnippetsSpec extends script.Specification with Snippets with DataTables with Grouped { def is = s2""" $sequential

 These are examples on how to use the various snippet methods

   + with the `snippet` method
   + with the `snippet` method and 2 lines
   + with the `snippet` method and cut comments
   + with the `snippet` method and cut comments - 2 blocks
   + with some code having accolades

Offsets
=======
 It is possible to specify an offset to the snippet
   with the `snippet` method
     + a positive offset
     + a negative offset

Trimming
========
 An approximated expression must
   + not include the `snippet` call
   + not have parameter setting calls

Results
=======
 Results can be displayed by using the `eval` method
   + when using the `snippet` method

Names
=====
 It is also possible to capture code names
   + for types (trait, classes,...)
     + with a fully qualified name
   + for method names
   + for method names with type parameters
   + for attribute names

Robustness
==========
 + A snippet must not fail if the code throws an exception
 + An interpolated snippet code must not be executed

                                                             """


  "snippets capture" - new group {

  eg := { s2""" code: ${ snippet { got {1 + 1} } } """.texts(1).t.trim === "`got {1 + 1}`" }
  def got[T](t: T) = t


  eg := s2""" code: ${ snippet {
got {
  var n = 0
  n = 1
}
} } """.texts(1).t.trim ===
    """|```
       |got {
       |  var n = 0
       |  n = 1
       |}
       |```""".stripMargin

  eg := s2""" code: ${ snippet {
// 8<--
var n = 0
// 8<--
n = 1
// 8<--
n = 0
} }""".texts(1).t.trim ===
    """|```
       |n = 1
       |```""".stripMargin

  eg := s2""" code: ${ snippet {
// 8<--
var n = 0
// 8<--
n = 1
// 8<--
n = 0
// 8<--
var i = 0
  } }""".texts(1).t.trim ===
    """```
      |n = 1
      |var i = 0
      |```""".stripMargin

   eg := s2""" code ${snippet { "e1" ! { ok } /**/;1/**/} }""".texts(1).t.trim === """`"e1" ! { ok }`"""
 }
  "offsets" - new group {
    eg := s2""" code: ${ snippet {
// 8<--
var n = 0
// 8<--
n = 1
// 8<--
n = 0
  }.offsetIs(2) }""".texts(1).t.trim ===
    """|```
       |  n = 1
       |```""".stripMargin

    eg := s2""" code: ${ snippet {
  // 8<--
  var n = 0
  // 8<--
  n = 1
  // 8<--
  n = 0
  }.offsetIs(-2) }""".texts(1).t.trim ===
    """|```
       |n = 1
       |```""".stripMargin

  }
  "trimming" - new group {
    eg := {
      "code"                   || "result" |>
        "snippet{ hello }"       !! "hello"  |
        " snippet{ hello }"      !! "hello"  |
        " snippet { hello }"     !! "hello"  |
        " snippet{ hello } "     !! "hello"  |
        " snippet{\n hello \n} " !! "hello"  |
        " snippet{ hello \n} "   !! "hello"  |
        { (c, r) => trimApproximatedSnippet(c) === r }
    }

    eg := {
      "code"                                      || "result" |>
        " snippet{ hello \n}.set(eval = true) "     !! "hello"  |
        " snippet{ hello \n}.eval "                 !! "hello"  |
        " snippet{ hello \n}.offsetIs(2) "          !! "hello"  |
        { (c, r) => trimApproximatedSnippet(c) === r }
    }
  }
  "results" - new group {
    eg := s2""" code: ${ snippet {
  var n = 1
  1 + n
  }.eval.offsetIs(-2) }""".texts.drop(1).take(2).map(_.t.trim).mkString("\n") ===
    """|```
       |var n = 1
       |1 + n
       |```
       |`> 2`""".stripMargin
  }
  "names" - new group {

    eg := {
      "code"                                         || "markdown"                 |>
      s"""the trait `${simpleName[Snippets]}`"""     !! "the trait `Snippets`"     |
      { (code, markdown) => texts(code)(0) === markdown}
    }

    eg := {
      "code"                                   || "markdown"                                      |>
      s"""the trait `${fullName[Snippets]}`""" !! "the trait `org.specs2.specification.Snippets`" |
      { (code, markdown) => texts(code)(0) === markdown}
    }

    eg := {
      "code"                              || "markdown"                                      |>
      s"""the method `${termName(is)}`""" !! "the method `is`"                               |
        { (code, markdown) => texts(code)(0) === markdown}
    }

    eg := {
      def function[T, S](t: T, s: S) = ""
      "code"                                                        || "markdown"                                      |>
      s"""the method `${termName(function(1, ""))}`"""              !! "the method `function`"                         |
      s"""the method `${termName(function[Int, String](1, ""))}`""" !! "the method `function`"                         |
      { (code, markdown) => texts(code)(0) === markdown}
    }

    eg := {
      "code"                                         || "markdown"                   |>
      s"""the attribute `${termName(attribute1)}`""" !! "the attribute `attribute1`" |
        { (code, markdown) => texts(code)(0) === markdown}
    }
  }
  "effects" - new group {
    eg := snippet[Unit](sys.error("boom")) must not(throwAn[Exception])

    eg := {
      var i = 0
      s2""" start ${snippet { i = 1; i }} end """
      i === 0
    }
  }

  def texts(fs: Fragments) = fs.texts.map(_.t).toIndexedSeq
  val attribute1 = 1
}

