# Rigorous Software Engineering Project 2020

## Changes

In case of changes to the project description, we will update the online version
of this description (but not the version that will be available in your group
repository). Please check the online version regularly.

Summary of changes:

- [no changes so far]

> NOTE: In contrast to the online version of this description, your group
> repository will contain working links to relevant repository files.

## Project Groups

The project group assignments can be found [here](https://lec.inf.ethz.ch/rse/2020/project/groups).

## Project Description

Consider the following class `TrainStation`:

```Java
public class TrainStation {

  private final int nTracks;
  private final Boolean[] occupied;

  public TrainStation(int nTracks) {
    this.nTracks = nTracks;
    this.occupied = new Boolean[nTracks];
    // all entries are initially false:
    for (int i = 0; i < nTracks; i++) {
      this.occupied[i] = false;
    }
  }

  public void arrive(int track) {
    // check TRACK_NON_NEGATIVE
    assert 0 <= track;
    // check TRACK_IN_RANGE
    assert track < this.nTracks;

    // check NO_CRASH
    assert !this.occupied[track];

    this.occupied[track] = true;
  }
}
```

The goal of the project is to implement a program analyzer that takes as input a
Java program (making use of the `TrainStation` class) and verifies that the
following conditions hold for this program:

Property TRACK_NON_NEGATIVE:

- For any **reachable** invocation of `arrive(track)` on an object `o` of class
  `TrainStation`, `track >= 0`.

Property TRACK_IN_RANGE:

- For any **reachable** invocation of `arrive(track)` on an object `o` of class
  `TrainStation`, `track < o.nTracks`.

Property NO_CRASH:

- For every object `o` of type `TrainStation`, every pair of calls
  `o.arrive(track1)` and `o.arrive(track2)` satisfy `track1 != track2`.

Your program analyzer (see skeleton below) must take as input a `TEST_CLASS` and
a `VerificationProperty` (e.g., `TRACK_NON_NEGATIVE`), and return true (called
`SAFE`) if the property is guaranteed to hold, and false (called `UNSAFE`) if
the property cannot be proven.

## Example 1

```Java
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Basic_Test_Safe {
  public static void m1(int j) {
    TrainStation s = new TrainStation(10);
    if (0 <= j && j < 10) {
      s.arrive(j);
    }
  }
}
```

## Example 2

```Java
// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Basic_Test_Unsafe {
  public void m2(int j) {
    TrainStation c = new TrainStation(10);
    if (-1 <= j && j <= 10) {
      c.arrive(j);
      c.arrive(j);
    }
  }
}
```

## Project Repository

For each group, we will set up a repository with a skeleton for your solution,
which you should see at
[https://gitlab.inf.ethz.ch/dashboard/projects](https://gitlab.inf.ethz.ch/dashboard/projects).
As a first step, you should read and follow the [README.md](/README.md) file. It
contains instructions on how to set up the project and run it.

The output of the skeleton is initially always `SAFE`, which is unsound. The
goal of the project is to follow the comments in the code (check for "FILL THIS
OUT"), such that the project only reports `SAFE` for test classes where we can
**guarantee** it (but as often as possible).

## Libraries

For your analysis, you will use the two libraries APRON and Soot. Part of your
assignment is understanding these libraries sufficiently to leverage them for
program analysis. In addition to the resources provided below, you may also
consult

- The course lectures on abstract interpretation and pointer analysis.
- The language fragment of Soot to handle (see below).
- The documentation for APRON and Soot (including documentation of methods and
  classes), which is available in Eclipse if you follow the instructions on setting up the
  project locally (see [README.md](/README.md) file).

### APRON

[APRON](http://apron.cri.ensmp.fr/library/) is a library for numerical abstract
domains. An example file of using APRON exists [here](./resources/Test.java) -
it should demonstrate everything you need to know about APRON. You can also find
documentation about the APRON framework
[here](./resources/apron-doc/index.html).

### Soot

Your program analyzer is built using [Soot](https://github.com/Sable/soot), a
framework for analyzing Java programs. You can learn more about Soot by reading
its
[tutorial](http://www-labs.iro.umontreal.ca/~dufour/cours/ift6315/docs/soot-tutorial.pdf),
[survivor guide](https://www.brics.dk/SootGuide/sootsurvivorsguide.pdf), and
[javadoc](https://www.sable.mcgill.ca/soot/doc/index.html). You can find
additional tutorials [here](https://github.com/Sable/soot/wiki/Tutorials).

Your program analyzer uses Soot's pointer analysis to determine which variables
may point to `TrainStation` objects (see the `Verifier.java` file in your skeleton).

### Language Fragment to Handle

For this project, you will analyse a fragment of Jimple. This language contains
only local integer variables and `TrainStation` objects. Note that the type of
integer variables can be int, byte, short, or bool (e.g., `int i = 10;` is
represented as byte, see also
[SootHelper.java](/analysis/src/main/java/soot/SootHelper.java) ->
`isIntValue`).

- Details about the Jimple language can be found
  [here](https://www.sable.mcgill.ca/soot/doc/index.html)
- The language fragment to handle is:

| Jimple Construct                                                                       | Meaning                                                                                                                                                                                                                                                    |
|----------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DefinitionStmt](https://www.sable.mcgill.ca/soot/doc/soot/jimple/DefinitionStmt.html) | Definition Statement: here, you only need to handle integer assignments to a local variable. That is, `x = y`, or `x = 5` or `x = EXPR`, where `EXPR` is one of the three binary expressions below. That is, you need to be able to handle: `y = x + 5` or `y = x * z`. |
| [JMulExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JMulExpr.html)    | Multiplication                                                                                                                                                                                                                                             |
| [JSubExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JSubExpr.html)    | Subtraction                                                                                                                                                                                                                                                |
| [JAddExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JAddExpr.html)    | Addition                                                                                                                                                                                                                                                   |
| [JIfStmt](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JIfStmt.html)      | Conditional Statement. You need to handle conditionals where the condition can be any of the binary boolean expressions below. These conditions can again only mention integer local variables or constants, for example: `if (x > y)` or `if (x <= 4)`, etc.  |
| [JEqExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JEqExpr.html)      | ==                                                                                                                                                                                                                                                         |
| [JGeExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JGeExpr.html)      | >=                                                                                                                                                                                                                                                         |
| [JGtExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JGtExpr.html)      | >                                                                                                                                                                                                                                                          |
| [JLeExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JLeExpr.html)      | <=                                                                                                                                                                                                                                                         |
| [JLtExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JLtExpr.html)      | <                                                                                                                                                                                                                                                          |
| [JNeExpr](https://www.sable.mcgill.ca/soot/doc/soot/jimple/internal/JNeExpr.html)      | != |

- Loops are also allowed in the programs.
- Assignments of pointers of type `TrainStation` are possible, e.g. `p = q`
  where `p` and `q` are of type `TrainStation`. However, those are handled by
  the pointer analysis.

## Implementation tips

- It is sufficient to analyze one method at a time.
- You can assume the constructor `TrainStation` takes as arguments only integer
  **constants** (never local variables), between -10000 and 10000.
- It is enough to use the polyhedra domain that
  [APRON](http://apron.cri.ensmp.fr/library/) provides (Polka) to analyze
  relations over the local integer variables.
- The methods of the control applications may contain loops and branches.
- If you see an operation for which you are not precise - do not crash, but be
  less precise or go to top instead so that you remain sound. This is useful in
  case of misunderstandings on the project description.
- You will need to apply widening. Do this after `WIDENING_THRESHOLD` steps (see
  [NumericalAnalysis.java](/analysis/src/main/java/ch/ethz/rse/numerical/NumericalAnalysis.java)).
- Only local variables need to be tracked for the numerical analysis (no global
  variables), but for the heap you need to use the existing pointer analysis of
  Soot. The skeleton already contains the invocation of the pointer analysis.
  You can then leverage the result of this pointer analysis for your numerical
  analysis.
- You can assume the analyzed code never throws exceptions, such as `null`
  dereferences, or division by zero.
- You can assume all analyzed methods only have integer parameters (in
  particular, they cannot have `TrainStation` parameters).
- You may ignore overflows in your implementation (in other words, you may
  assume that APRON captures Java semantics correctly)
- The three properties to check are ordered by difficulty. We recommend you work
  on them in order.
- To check `NO_CRASH`, it may help to introduce an imaginary track `-1` which is
  occupied originally (there may be more precise approaches, but we do not
  expect you to implement those).
- We strongly recommend you to test your implementation on many examples (you
  should come up with your own examples).

## Deliverables

- The project deadline is **Thursday, June 5th, 2020, 17:00**!
- We may decline to answer project questions after Tuesday, June 3rd, 2020,
  17:00. This avoids last-minute revelations that cannot be incorporated by all
  groups.
- Commit and push your project to the master branch of your
  [GitLab](https://gitlab.inf.ethz.ch/) repository (that originally contained
  the skeleton) before the project deadline. **Please do not commit after the
  deadline** - we will flag groups that try this.
- If you cannot access your GitLab repository, contact
  benjamin.bichsel@inf.ethz.ch.
- Your project must use the setup of the provided skeleton. In particular, you
  cannot use libraries other than those provided in
  [analysis/pom.xml](/analysis/pom.xml).
- There will be a limit of 10 seconds and 8G to verify an application (already
  set up in the skeleton). Each application will consist of at most 10 methods.
- We will award **bonus points** for groups that achieve an instruction coverage
  of `>=75%`.

## Grading

- We will evaluation your tool on our own set of programs for which we know if
  they are valid or not.
- We will evaluate you depending on the correctness of your program and the
  precision of your solution. You will not get points if your program does not
  satisfy the requirements. If your solution is unsound (i.e. wrong - says that
  an unsafe code is safe), or imprecise (says unsafe for code, which is safe) we
  will penalize it.
- We will penalize unsoundness much more than imprecision.
- Your solution must use abstract interpretation, do not use other techniques
  like symbolic execution, testing, random guessing, machine learning, etc.
- Do not try to cheat (e.g., by reading the solutions from the test file)!

## Project Assistance

For questions about the project, please consult (in this order):

- This project description
- The skeleton in your GitLab repository (in particular the README file)
- The documentation of libraries&frameworks, in particular APRON and Soot
  discussed above
- The [Moodle
  page](https://moodle-app2.let.ethz.ch/mod/forum/view.php?id=415836). All
  students will see and are encouraged to reply to the questions about the
  project.
- The project TA at benjamin.bichsel@inf.ethz.ch (only when Moodle is not possible)
