
# Maia <img src="../master/docs/logos/maia.png?raw=true" height="35">

*Highly-typeful data fetching in Scala.js, a la GraphQL*

[![Build
Status](https://travis-ci.org/MaiaOrg/scala-maia.svg?branch=master)](https://travis-ci.org/MaiaOrg/scala-maia)
[![Stories in
Ready](https://badge.waffle.io/MaiaOrg/maia-core.svg?label=ready&title=Ready)](http://waffle.io/MaiaOrg/maia-core)

## Welcome

Maia is a Scala library for the JVM and Javascript platforms that enables
application authors to share structured data between processes using a
well-typed query language. It shares similar goals with the larger [GraphQL
project](http://graphql.org/) but is optimized for the Scala platform and
ecosystem and offers more type-level guarantees to users for that restriction.

**Looking for more maintainers.** If you're interested in Scala, GraphQL,
APIs, and/or type-level programming then [contact me](mailto:me@jspha.com).

## Etymology

Maia is Greek for "midwife" and name of the eldest of the Pleiades, but more
directly it refers to the maieutic, or socratic, method---a didactic method of
having a discussion through a series of questions and answers.

## Contributing

### Short of it

- Contributors welcome. Contribute a comment, identify a problem, submit
  a PR.
- Any PR which solves a "real, recognized problem" and passes tests gets
  merged: no questions asked.
  - If you cause a new "real, recognized problem" then someone can
    submit a new patch to fix it.
- "Real, recognized problems" are tracked in Github Issues and on the
  [Waffle.io board](http://waffle.io/MaiaOrg/maia-core).

### Deeper dive

Maia is follows a [C4-like](https://rfc.zeromq.org/spec:42/C4/)-like
contribution system. As Maia is still in the early stages, so this stuff
isn't all in place quite yet, but if you'd like to contribute then we'll
move to make it easy for you.

Generally, C4 implies a commit-first-ask-questions-later style
development process. Submit a PR and if it solves a real problem and
passes tests then a Maintainer will merge it quickly and without further
debate.

### Patch guidelines

- Patches must pass continuous integration on the Travis CI instance associated
  with this Github repository.
- Code must be formatted according to the ScalaFmt configuration provided.
- Patches must pass test suite which includes WartRemover and ScalaStyle lints,
  e.g.
  - Tabs are disallowed
  - Whitespace at end of lines is disallowed
  - MPLv2.0 license must be the header of every file
  - Public methods must be given an explicit type
  - Type-unsafe `==` is disallowed
  - `return`, `null`, `.clone()`, and `.finalize()` are disallowed
  - Structural types are disallowed
  - &c.
    - For more, see [Wartremover](https://github.com/wartremover/wartremover)
      and `scalastyle-config.xml`

### Maintainers

- Joseph Abrahamson <me@jspha.com>

## License, generally

Code should be licensed under the Mozilla Public License Version 2.0.  The MPL
operates on a file-by-file basis. Commits adding new files should include the
following MPL license header at the top of the file

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/.

