# Banking Transaction Processor

A small service that handles deposits, withdrawals and transfers across multiple accounts, keeps a
ledger of every transaction, and lets you query balances and history.

Core Java only. No build tool, no libraries, no test framework.

## Running it

You need Java 17 or newer. From the project root:

```bash
javac -d out/classes $(find datamodel/src/main/java common/src/main/java src/main/java src/test/java -name "*.java")
```

Run the tests:

```bash
java -cp out/classes com.banking.system.TestSuite
```

Or the console app, if you'd rather click through it by hand:

```bash
java -cp out/classes com.banking.system.BankingSystemApplication
```

Individual suites run on their own too, for example `com.banking.system.TransferTest`.

## The idea I built around

Most of the requirements are easy to satisfy on the surface. The one I kept coming back to is this:

> The ledger and the balance should never disagree.

If those two can drift apart, nothing else in the system means much. "Prevent overdrafts" isn't
really a validation rule either. It's a question of which states the system can ever reach. A
balance check that passes, and then gets acted on a moment later, isn't a guarantee.

Most of the decisions below come out of that one idea.

## How it's laid out

Three modules, so the compiler enforces the dependency direction instead of me remembering it:

- **`datamodel`** — `Account`, `Transaction`, the enums, the exceptions. Depends on nothing.
- **`common`** — validation, repositories, the in-memory stores, the services, the lock manager.
- **`src`** — the console app and the tests.

Inside `common` it runs service → repository → store. `BankingServiceValidator` holds the validation
rules, and `BankingSystemFactory` is the only place that knows how to wire things together, so the
app and the tests build the system the same way.

`Account` isn't just a bag of fields. It owns its balance and the operations on it, so nothing can
move money without going through it. What it doesn't own is validation or transaction recording,
because both of those need the repositories, and I didn't want to drag those into an entity.

## The decisions

**Money is `BigDecimal`, never `double`.** Floating point can't represent 0.10 exactly, and I'd
rather be slow than wrong about someone's balance. Comparisons use `compareTo`, so 10 and 10.00
count as the same amount.

**The ledger lives in its own store.** An account keeps the ids of the transactions that touched it;
the transaction store keeps the transactions themselves. I could have hung `Transaction` objects
straight off `Account`, but then accounts grow forever, and you can't ask anything about
transactions globally without walking every account.

**`getTransactionHistory` gives you transactions, not ids.** If it handed back ids, every caller
would have to go and look them up before it could show anything useful. That's my job, not theirs.

**The history you get back can't be modified.** Otherwise a caller could append to someone else's
ledger by accident. The stored list is copy-on-write, and reads hand out a snapshot.

**`Transaction` is immutable, `Account` isn't.** A ledger entry records something that already
happened, so it checks itself on construction and never changes afterwards. A balance is live state,
so `Account` owns it.

**Locking is per account.** `ConcurrentHashMap` keeps the map safe, but it does nothing about
reading a balance, deciding, and writing it back. Without a lock, two threads can both pass the same
check and both withdraw. I measured it: 2000 concurrent withdrawals of 1 against a balance of 1000
let 1046 of them through, and the balance still showed zero. So 46 units appeared out of nowhere,
and the ledger no longer matched the account.

`AccountLockManager` hands out one lock per account id, and the locked section covers the balance
change *and* the ledger write, so you can't catch them out of step. Transfers take both locks in
account-id order, which means two transfers going in opposite directions can't deadlock.

I considered two alternatives. A single global lock is simpler and also correct, but it makes
unrelated accounts queue behind each other. `AtomicReference` with compare-and-swap makes the
balance atomic, but I couldn't stretch it to cover the ledger write, which was the part I cared
about.

**Validate everything first, then change something.** No operation touches state until all its
checks have passed, so there's nothing to roll back. In a transfer the balance check happens inside
the lock, and only then do both balances move and both ledger entries get written. Nothing after
that point can throw.

**Creating an account is atomic.** `putIfAbsent` instead of check-then-save, so two threads racing
on the same id can't both succeed.

**"APIs" meant Java interfaces to me, not HTTP.** The brief asks for APIs to query balances and
history, and I read that as the `BankingService` contract. It's fair to push back on that.
`com.sun.net.httpserver` is in the JDK, so I could have added HTTP without breaking the
no-libraries rule. I didn't, because hand-writing a request router and JSON would have eaten the
time I wanted for the ledger itself. The console app covers that gap instead.

**I made it thread-safe.** My reasoning: "prevent overdrafts" is trivially true with one
thread and stops being true with two, and a transaction processor for a bank isn't a single-threaded
thing. It cost a lock manager and a whole test suite. I'd rather pay that than hand over a ledger
that can invent money.

**Errors are an enum.** `BankingSystemError` keeps the codes (BP1001–BP1008) and the wording in one
place, and the exceptions carry them. A little more indirection at each throw, but the messages
can't drift apart.

**Java 17, not something newer.** There's no build file to declare a source level, so whichever JDK
you open this with becomes the source level. Rather than assume a recent one, the code stays inside
Java 17 and the IntelliJ language level is pinned there. I ran it on 17 and on 25.

**No JUnit and no build tool, as required.** `TestAssertions` has the assertions, `TestRunner` runs
a suite and reports it, and `TestSuite` runs all six and exits non-zero if anything fails. A suite
reports every failure it finds, not just the first. What I gave up: parameterised tests, setup
methods, the IDE test tree, mocking. What I got back: it runs anywhere with `javac` and `java`.
`TestFixtures` does the work setup methods would have.

**An opening balance is recorded as a `DEPOSIT`.** It needs a ledger entry, or the balance and the
ledger disagree from the start. A separate `OPENING_BALANCE` type would be more accurate, since an
opening balance isn't really a customer deposit, but the requirements don't ask for one so I didn't
add it.

## Tests

76 tests, six suites, one command.

| Suite | Tests | What it covers |
| --- | --- | --- |
| `AccountCreationTest` | 13 | ids, duplicates, opening balances, isolation |
| `DepositTest` | 9 | credits, invalid amounts, unknown accounts |
| `WithdrawalTest` | 11 | debits, exact balance, overdrafts |
| `TransferTest` | 15 | both sides moving, same account, null and blank ids, overdrafts |
| `TransactionHistoryTest` | 22 | ledger contents, ordering, per-account isolation, immutability |
| `ConcurrencyTest` | 6 | the invariant, under 16 threads |

I've tried to make the test names readable as a description of what the service promises, and each
assertion says what behaviour it's protecting rather than just comparing two values.

`ConcurrencyTest` is the one I'd point at first. It fires 2000 concurrent withdrawals at a balance
of 1000 and checks that exactly 1000 succeed, that the balance lands on zero, and that the ledger
has one entry per successful withdrawal. It also replays the ledger arithmetically to see whether it
reproduces the balance, runs 1000 transfers in opposing directions to check the lock ordering holds
up, and has 200 threads race to create the same account id to confirm only one wins.

## Edge cases I thought about

Null, empty and blank account ids. Null, zero and negative amounts. Unknown accounts on every
operation. Duplicate ids, including two threads at once. Withdrawing or transferring the exact
balance. Transferring to the same account. Opening an account at zero, which shouldn't create a
ledger entry. Failed operations leaving both the balance and the ledger untouched. Amounts written
at different scales, like 10 against 10.00. Transaction ids that don't exist coming back as nothing
rather than as nulls in a list.

## What it doesn't do

- **It's all in memory.** Everything goes when the process exits. The repository interfaces are
  there so a real store could slot in without changing the services.
- **One JVM only.** The locking is in-process. Across processes you'd want database transactions or
  optimistic locking on a version column instead.
- **The lock map only grows.** One lock per account id, never reclaimed. Fine here, wrong for a
  long-running process with a lot of account churn.
- **Timestamps come from `System.currentTimeMillis()`**, not an injected `Clock`. So anything
  time-dependent is checked against a window rather than pinned, and two transactions in the same
  millisecond are only ordered by insertion.
- **No money or currency type.** Amounts are plain `BigDecimal` with no scale or rounding policy, so
  nothing stops you mixing precisions, or currencies in a system that had more than one.
- **History comes back whole.** No date range, no filter, no limit. A real account wouldn't return
  its entire life in one call.
- **`Transaction` has no `equals` or `hashCode`.** Nothing needs it yet; the tests compare ids.
- **Rejected operations aren't recorded.** A failed withdrawal correctly leaves no ledger entry, but
  a real system would want the attempt logged somewhere.

## If I had more time

1. Inject a `Clock`, and make the ordering of same-millisecond transactions explicit.
2. Add a `Money` type holding amount, scale and currency, and stop passing raw `BigDecimal` around.
3. Add an `OPENING_BALANCE` transaction type instead of overloading `DEPOSIT`.
4. Add paging and filtering to the history query.
5. Swap the in-memory stores for something persistent behind the existing interfaces, at which point
   the in-process locks give way to real transactions.
6. Add an audit log to record unsuccessful transactions.

## A note on the tests and the commits

I wrote the tests first, ahead of each piece of implementation. What the history doesn't show is the
individual red-green steps, because I grouped the commits one per feature rather than one per cycle,
so a feature's tests and its code land together. The commits after those are a review pass over the
whole thing.

If you'd rather read the behaviour than the code, `TestSuite` is the quickest way in. It's 76 test
names describing what the service promises.
