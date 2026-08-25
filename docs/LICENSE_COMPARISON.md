# LICENSE comparison — MIT vs. Apache 2.0

Prepared 2026-08-25 to support the open "LICENSE choice" item in
[`docs/NEEDS_YOUR_INPUT.md`](NEEDS_YOUR_INPUT.md). **This document does not pick one — that's
explicitly the project owner's call, per that file's own framing ("ask, don't guess") and
`docs/DECISIONS.md`'s general pattern of not making irreversible, owner-scoped decisions
quietly.** It lays out the factual differences between the two license options
`NEEDS_YOUR_INPUT.md` already named as the common permissive defaults for a project like this one,
so the choice can be made with the actual terms in view rather than by name recognition alone.

Both are checked directly against their real license text and against the Open Source
Initiative/`choosealicense.com` summaries of each (not assumed from general familiarity) — see
"Sources" at the end.

## The short version

Both MIT and Apache License 2.0 are **permissive** open-source licenses — the same broad
category, and the most common one for exactly this kind of project (a personal app the owner
intends to publish and let others use/modify freely). Neither is a "copyleft" license like the
GPL: neither requires anyone who modifies and redistributes Oumatjie's code to release their own
changes under the same license, or to release source code at all. If the goal is "let people use
and build on this with minimal friction, and don't try to control what they do with it
afterward," either license achieves that goal. The real differences are narrower than they might
sound.

## Side-by-side

| | MIT | Apache License 2.0 |
|---|---|---|
| Commercial use | Permitted | Permitted |
| Modification | Permitted | Permitted |
| Distribution | Permitted | Permitted |
| Private use | Permitted | Permitted |
| Must preserve copyright/license notice | Yes | Yes |
| Must state changes made to the code | No | Yes — modified files must carry a "prominent notice" that they were changed |
| Explicit patent grant from contributors | No (silent on patents) | Yes — an express, perpetual, worldwide patent license from each contributor, which terminates against anyone who sues over patents on the covered code |
| Trademark rights granted | No (silent — MIT doesn't mention trademarks at all) | Explicitly does **not** grant any trademark rights (the license says so directly) |
| Warranty / liability | Disclaimed ("as is," no warranty, no liability) | Disclaimed ("as is," no warranty, no liability) |
| License text length | About 170 words | Several pages — meaningfully longer and more formal |
| Typical use case | Small/personal projects, libraries, "just let people use this" | Projects that might attract outside contributors, or where patent exposure is a realistic concern |

## What actually differs, in plain terms

**Length and simplicity.** MIT is a handful of sentences. Apache 2.0 is a full legal document
with defined terms, numbered sections, and more explicit handling of edge cases (contributions,
patents, trademarks, notices). For a single-owner personal project like Oumatjie, MIT's brevity is
a real, if modest, advantage — anyone glancing at the repo can read the whole license in under a
minute. Apache 2.0's extra length exists to cover situations (multiple contributors, corporate
use, patent portfolios) that may or may not ever apply here.

**The patent grant is the most substantive legal difference.** Apache 2.0 has each contributor
explicitly grant a patent license to anyone who uses the code — meaning if a contributor (or the
original owner) later holds a patent that reads on something in this codebase, users of the
Apache-licensed code are protected from being sued over it, as long as they don't sue the project
over patents themselves first (the grant terminates if the user initiates patent litigation over
the covered work). MIT says nothing about patents at all, which is a genuine, if usually
theoretical, gap for MIT — courts and lawyers have debated for years whether an MIT license
implies a patent license the way Apache's does explicitly. For a personal email app with no known
patented technique in it, this is unlikely to matter in practice, but it's the single clearest
substantive difference between the two, not just a style choice.

**"State changes" is Apache 2.0's one real extra obligation.** Apache 2.0 requires anyone who
modifies and redistributes the code to mark which files they changed. MIT has no equivalent
requirement — someone can take MIT-licensed code, modify it, and redistribute it with no marker
of what changed, as long as the original copyright/license notice is preserved somewhere. This is
a real (if light) extra duty Apache 2.0 places on downstream users that MIT doesn't.

**Trademark handling.** Apache 2.0 explicitly states it does not grant trademark rights — useful
if "Oumatjie" as a name/brand ever matters distinctly from the code itself (a trademark is a
separate legal instrument from a copyright license regardless of which license is chosen, but
Apache 2.0 says so in the license text; MIT is simply silent on the subject).

**Compatibility.** Both are compatible with being combined with most other permissive- and
copyleft-licensed code in the broader ecosystem (Apache 2.0 is explicitly GPLv3-compatible one
direction; MIT is compatible with nearly everything given how minimal its terms are). This is
unlikely to matter for Oumatjie specifically unless a future contributor wants to pull in code
from an incompatible source, which isn't a live concern today.

## What's the same either way

Both fully permit exactly what the project owner has already described wanting: making the repo
public, letting others use, modify, and build on the code, commercially or not, with attribution
as the only real ask. Both disclaim all warranty and liability equally — nobody using Oumatjie's
code under either license could hold the project owner responsible if something in it breaks or
causes harm. Neither restricts what license a modified/derivative version must use (unlike GPL),
so a fork could relicense its own changes under different terms in either case. Neither requires
publishing source code for a modified version at all — someone could take Oumatjie's code (MIT or
Apache) and ship a closed-source app built on it, and both licenses allow that equally.

## For reference: what a third option would look like

`NEEDS_YOUR_INPUT.md` also mentions GPL as a real (if less likely) alternative. Briefly, since
it's a materially different kind of license, not just a variant of the above: GPL is copyleft —
anyone who distributes a modified version must also release their modified source code under the
same GPL terms. That's a meaningfully different philosophy (source availability is enforced
downstream, not just offered) from either MIT or Apache 2.0, and would be a bigger decision than
choosing between the two permissive options above. Not elaborated further here since nothing
about this project's stated goals ("let people use this freely") points toward wanting that
enforcement — but flagged in case it's actually wanted.

## Sources

- [MIT License — choosealicense.com](https://choosealicense.com/licenses/mit/)
- [Apache License 2.0 — choosealicense.com](https://choosealicense.com/licenses/apache-2.0/)
- The Apache License 2.0 full text and the MIT License full text, both standard, unmodified forms
  as published by the Open Source Initiative / Apache Software Foundation.
