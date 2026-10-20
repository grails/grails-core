---
name: grails-9-upgrade
description: Guide for upgrading Grails applications from Grails 8.x to Grails 9, covering the Groovy 6 baseline, the GPars deprecation in favour of groovy.concurrent, invokedynamic compilation, the Jackson-backed JSON converter and domain class serializer, RxGORM on RxJava 3 (imports, blocking calls, subscribers, empty results), and the deprecated RxJava 1 and RxJava 2 async and events modules
license: Apache-2.0
---

<!--
SPDX-License-Identifier: Apache-2.0

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements; and to You under the Apache License, Version 2.0.
-->

## What I Do

- Guide upgrades of Grails applications from Grails 8.x to Grails 9.
- Turn the Grails 9 upgrade guide into a practical migration checklist.
- Identify the Groovy 6, GPars, invokedynamic, JSON rendering, RxGORM, and RxJava changes that can break an application.
- Keep migration work focused on public application behavior, not internal Grails framework implementation details.

## When to Use Me

Activate this skill when:

- Updating an application to Grails 9.
- Reviewing a Grails 9 upgrade branch or pull request.
- Fixing tests, build failures, runtime failures, or behavior changes after moving from Grails 8.x to Grails 9.
- Porting code that uses RxGORM, `grails-async-rxjava`, `grails-async-rxjava2`, `grails-events-rxjava`, or `grails-events-rxjava2` to RxJava 3.
- Replacing GPars (`grails-async-gpars`, `grails-events-gpars`, or direct `GParsPool` use) with Groovy's `groovy.concurrent` toolkit.
- Checking an application's JSON output before or after setting `grails.converters.json.legacy` to `false`.

For an application still on Grails 7, apply the `grails-8-upgrade` skill first and move to Grails 8 before using this one.

## Primary Sources

Use the published Grails documentation as the source of truth before changing an application. The URLs below use `<version>`, which stands for the Grails 9 version the application is upgrading to, such as the `grailsVersion` in its `gradle.properties`; use `snapshot` for a snapshot version.

| URL | Use for |
|-----|---------|
| `https://grails.apache.org/docs/<version>/guide/upgrading.html#upgrading90x` | Main Grails 8 to Grails 9 upgrade guide |
| `https://grails.apache.org/docs/<version>/guide/introduction.html#whatsNew` | Grails 9 feature and platform overview |
| `https://grails.apache.org/docs/<version>/guide/introduction.html#dependencyUpgrades` | Platform dependency baseline |
| `https://grails.apache.org/docs/<version>/guide/async.html` | Promises and events, including the RxJava 3 modules |
| `https://grails.apache.org/docs/<version>/guide/theWebLayer.html` | JSON rendering and the converters |
| `https://groovy-lang.org/releasenotes/groovy-6.0.html#concurrency-toolkit` | The `groovy.concurrent` toolkit that replaces GPars |
| `https://grails.apache.org/docs/<version>/ref/Versions/Grails%20BOM.html` | Dependency versions of the Grails BOM |
| `https://groovy-lang.org/releasenotes/groovy-6.0.html` | Groovy 6 language and runtime changes |
| `https://github.com/ReactiveX/RxJava/wiki/What's-different-in-3.0` | RxJava 3 API differences, with the RxJava 1 to 2 differences it links to |

## Upgrade Baseline

Verify these before touching application code; `gradle.properties`, the Gradle wrapper, and the Grails BOM are the sources of truth:

- Java 21 remains the minimum to build and run.
- Groovy moves from 5.x to 6.0, and Spock to the `groovy-6.0` variant. Do not pin `groovy-all` or Spock yourself; the Grails BOM manages both.
- Spring Boot 4.1 and Spring Framework 7 are unchanged from Grails 8; a Grails 8 application's Boot configuration carries over.
- Gradle 9.8 or later for the wrapper.
- RxGORM (`grails-datamapping-rx`) depends on `io.reactivex.rxjava3:rxjava`. The RxJava 1 artifact `io.reactivex:rxjava` is no longer transitively available through it.
- Groovy source sets compile with invokedynamic by default; `grails { indy = false }` was the Grails 8 default and is now unset.
- `grails.converters.json.legacy` defaults to `true`, so JSON renders as it did in Grails 8 until the application opts in to the Jackson-backed converter.

## Migration Workflow

1. Update `grailsVersion` in `gradle.properties` and refresh the Gradle wrapper; let the Grails BOM manage Groovy, Spock, and RxJava versions.
2. Build without tests first (`./gradlew compileGroovy compileTestGroovy`) and fix compilation errors module by module.
3. Replace GPars with `groovy.concurrent`, following the sections below.
4. Port RxGORM and RxJava code, following the sections below.
5. Replace the deprecated RxJava 1 and 2 async and events modules with their RxJava 3 counterparts.
6. Check the application's JSON with `grails.converters.json.legacy` set to `false`, then set it explicitly either way.
7. Run the full test suite, then start the application and read its log for `[DEPRECATED]` and `deprecated` warnings from Grails.
8. Review the published upgrade guide section by section, confirming every item either does not apply or has been handled.

## Groovy 6

Groovy 6 is a major Groovy release. Read its release notes before assuming that a Groovy 5 construct behaves the same way. Compile the application statically where it was already static; a construct that compiles dynamically but fails under `@CompileStatic` is a sign of a Groovy change rather than a Grails one. Keep the Spock version the Grails BOM supplies, which is built for Groovy 6.

## GPars Deprecated in Favour of Groovy Concurrency

Groovy 6 brings the concurrency and parallel-processing patterns of GPars into the language as `groovy.concurrent`, so Grails no longer needs the separate GPars library. `grails-async-gpars` and `grails-events-gpars` still work in Grails 9, carry `@Deprecated(since = '9.0', forRemoval = true)`, log a warning when selected, and will be removed in a future release.

Replace the dependency, one for one:

| Grails 8 | Grails 9 |
|----------|----------|
| `org.apache.grails.async:grails-async-gpars` | `org.apache.grails.async:grails-async-groovy-concurrent` |
| `org.apache.grails.events:grails-events-gpars` | `org.apache.grails.events:grails-events-groovy-concurrent` |

Do not leave both on the classpath: only the first service-loaded `PromiseFactory` is used, and the event bus builder refuses to start when it finds more than one `EventBus` implementation.

The Groovy promise factory follows the failure contract of the default `CompletableFuture` factory rather than the GPars one. A direct or chained `get()` throws `ExecutionException` with the original failure as its cause, a single-promise `onError` recovers with its callback result, the aggregate `onError` observes the failure and leaves the returned promise failed, and `onComplete` and `then` return a new promise instead of the receiver. A test that asserted the GPars behaviour needs updating.

The Groovy actor event bus delivers each event to its subscribers in publication order on one actor. A subscriber that throws without a reply handler is logged and does not stop delivery to the remaining subscribers or to later events.

Application code that uses GPars directly moves to the Groovy equivalents:

| GPars | `groovy.concurrent` |
|-------|---------------------|
| `GParsPool.withPool { list.eachParallel { } }` | `list.eachParallel { }`, which needs no pool |
| `GParsPool.withPool(n) { list.collectParallel { } }` | `ParallelScope.withPool(n) { scope -> list.collectParallel { } }` |
| `GParsConfig.setPoolFactory(..)` | `Awaitable.setExecutor(..)`, which also hosts the actor event bus |

Promises the Groovy factory creates are Groovy `Awaitable` instances, so they can be passed to `await` and to combinators such as `Awaitable.all` without conversion. Once the deprecated modules are removed, Grails no longer brings GPars onto the classpath, so an application that still needs it declares `org.codehaus.gpars:gpars` itself.

Groovy marks the toolkit as incubating. The Grails modules on top of it expose only the Grails `Promise` and `EventBus` contracts, so later Groovy changes are absorbed by Grails rather than by application code.

## Groovy Code Compiles with invokedynamic

Grails 8 compiled every Groovy source set without invokedynamic, because `grails { indy = false }` was the default. Grails 9 leaves the setting unset, so the Groovy compiler's own default applies, and on Groovy 6 that is invokedynamic.

- Remove `grails { indy = true }`; it is now the effective default.
- Build logic that read the property must allow for it having no value: use `grails.indy.getOrElse(true)` or check `grails.indy.present` instead of `grails.indy.get()`.
- An application that sets `indy = false` to keep classic call sites must also declare `implementation 'org.apache.groovy:groovy-callsite'` itself, at the BOM's version, and again for every source set that does not see implementation dependencies, such as `java-test-fixtures` test fixtures. Without it the Groovy compiler stops with a message naming the module.
- An application that applies GraalVM's native image plugin cannot set `indy = false`; the build fails with the reason, because an image needs invokedynamic.
- A library compiled with classic call sites needs `groovy-callsite` whatever the application's setting, or loading one of its classes fails with `NoClassDefFoundError: org/codehaus/groovy/runtime/callsite/CallSiteArray`. Spock 2.5, Geb 8 and the Asset Pipeline 5.2 are compiled with invokedynamic; `org.testcontainers:testcontainers-spock` is not, so declare `testRuntimeOnly 'org.apache.groovy:groovy-callsite'` next to it.

## JSON Rendering Can Use Spring Boot's JsonMapper

`grails.converters.JSON` still renders as it did in Grails 8 by default, because `grails.converters.json.legacy` defaults to `true`. Grails logs a warning once at startup while the setting is unset, because a later release makes `false` the default. Set it explicitly either way to silence the warning, and check the application's JSON with `false` before that release:

```yaml
grails:
    converters:
        json:
            legacy: false
```

With `legacy: false`, the converter writes through the application's Jackson `JsonMapper`, so `spring.jackson.*` settings and registered Jackson modules apply. Check these values in the application's JSON contract and in any test that asserts JSON text, because they render differently: `OffsetTime`, `Month` (a number, not its name), `Locale` (a language tag), `char` and `Character`, `byte[]` and `char[]` (Base64 and a string), `UUID`, `URI` and other `@JsonValue` types (their value, not a bean), `Pattern`, `Charset`, `InetAddress`, `File`, `Path`, `ByteBuffer`, `JsonNode`, `Optional` (its value or `null`), a `NaN` or infinite `double` (a string), and the layout of pretty printed JSON. Enums still render by `name()`, and strings are still escaped for an HTML `<script>` element, deliberately differing from Spring Boot.

Parsing does not change: `JSON.parse`, `request.JSON` and `new JSONObject(String)` accept the same syntax and return the same types.

A marshaller an application registers still takes precedence over the mapper, so keeping one type's Grails 8 rendering under `legacy: false` is a matter of registering a marshaller for it, for example in `BootStrap.init`:

```groovy
JSON.registerObjectMarshaller(Month) { Month month -> month.name() }
```

Subclassing `org.grails.web.json.JSONWriter` needs review whatever the setting: it now writes through a Jackson `JsonGenerator`, its `array()`, `object()`, `key()` and `value` methods no longer call the protected `append` and `comma` methods, and its text reaches the `Writer` as the buffer fills rather than per value. `PrettyPrintJSONWriter` and the JavaCC parser in `org.grails.web.json.parser` are deprecated.

With `legacy: false`, domain classes render through `DomainClassSerializer`, which honours `@JsonIgnore` and `@JsonProperty(access = WRITE_ONLY)` where the marshaller did not, so a property that was rendered before may now be absent. Set `grails.converters.json.domain.jackson.enabled` to `true` to have the application's own `JsonMapper` render domain classes the same way, for example when a Spring MVC controller returns one.

## RxGORM on RxJava 3

RxGORM returns RxJava 3 types. Every `rx.Observable` becomes `io.reactivex.rxjava3.core.Observable`, and an `@RxSchedule` service method that returned `rx.Single` returns `io.reactivex.rxjava3.core.Single`.

### Imports

| RxJava 1 | RxJava 3 |
|----------|----------|
| `rx.Observable` | `io.reactivex.rxjava3.core.Observable` |
| `rx.Single` | `io.reactivex.rxjava3.core.Single` |
| `rx.Subscriber` | `io.reactivex.rxjava3.core.Observer` |
| `rx.Subscription` | `io.reactivex.rxjava3.disposables.Disposable` |
| `rx.Scheduler`, `rx.schedulers.Schedulers` | `io.reactivex.rxjava3.core.Scheduler`, `io.reactivex.rxjava3.schedulers.Schedulers` |
| `rx.functions.Func1`, `Func2`, `FuncN` | `io.reactivex.rxjava3.functions.Function`, `BiFunction`, `Function<Object[], R>` |
| `rx.observers.TestSubscriber` | `io.reactivex.rxjava3.observers.TestObserver` |

A declared return type such as `rx.Observable<Book>` in a `@Service` interface must change too, or the service implementer no longer recognises the method as reactive.

### Blocking Calls

| RxJava 1 | RxJava 3 |
|----------|----------|
| `observable.toBlocking().first()` | `observable.blockingFirst()` |
| `observable.toBlocking().single()` | `observable.blockingSingle()` |
| `observable.toList().toBlocking().single()` | `observable.toList().blockingGet()` |
| `single.toBlocking().value()` | `single.blockingGet()` |
| `observable.toBlocking().first()` compared with `null` | `observable.firstElement().blockingGet()`, which returns `null` when empty, or expect `blockingFirst()` to throw `NoSuchElementException` |

`Observable.toList()` returns a `Single<List<T>>` in RxJava 3. The RxGORM methods that emit a list, such as `list()`, `DetachedCriteria.toList()`, criteria `list()` and `toListObservable()` on a lazy association, still return `Observable<List<T>>`, so subscribers to them do not change.

### Subscribers

- Closure subscriptions, `observable.subscribe { Book book -> ... }` and the two-closure `subscribe(onNext, onError)`, are unchanged.
- A full subscriber implements `Observer<T>`: add `onSubscribe(Disposable d)`, rename `onCompleted()` to `onComplete()`, and keep the `Disposable` to cancel instead of calling `unsubscribe()` on a `Subscription`.
- `DetachedCriteria`, `ObservableProxy` and the lazy association collections implement `ObservableSource<T>`. Their `subscribe(Observer)` returns `void`; code that kept the returned `Subscription` keeps the `Disposable` from `onSubscribe` instead. They can be passed to `Observable.wrap(..)`, `concatWith(..)` and any other operator that takes an `ObservableSource`.
- `TestSubscriber` tests become `TestObserver` tests: `awaitTerminalEvent(..)` is `await(..)`, `assertCompleted()` is `assertComplete()`, and `onNextEvents` is `values()`.

### Empty Results and Null

RxJava 3 never emits `null`.

- A query with no result completes empty, as before.
- `save()`, `insert()` and `persist()` on an instance the datastore did not write complete empty instead of emitting `null`.
- An `@RxSchedule` service method returning `Single` whose body returns `null` signals `java.util.NoSuchElementException`; one returning `Observable` completes empty. Where a missing result is expected, declare `Observable` and use `firstElement()`, which gives a `Maybe` whose `blockingGet()` returns `null` when empty, rather than catching the exception. RxJava 3's own zero-or-one type, `Maybe`, is not yet recognised as a data service return type.
- A `switchMap` or `flatMap` function must return an `ObservableSource`, never `null`; use `Observable.empty()`.

### Datastore Implementations

A datastore implementation of RxGORM compiled for Grails 8 or earlier, such as the RxGORM for MongoDB plugin, links against RxJava 1 and fails at runtime with `NoClassDefFoundError: rx/Observable`. It must be rebuilt against this release. The `rx-mongodb` profile feature is removed, because the plugin it generated has had no release since GORM 6.1 and was never published to Maven Central. `RxEntity` has had no datastore implementation for several releases, so an application that implements it should move to `GormEntity` with the RxJava data services, which work on every GORM datastore.

## Deprecated RxJava 1 and RxJava 2 Modules

`grails-async-rxjava`, `grails-async-rxjava2`, `grails-events-rxjava` and `grails-events-rxjava2` are deprecated for removal. They still work and log a warning such as `The RxJava 1 promise factory (grails-async-rxjava) is deprecated` when the promise factory or event bus is created.

- Replace `org.apache.grails.async:grails-async-rxjava` or `grails-async-rxjava2` with `grails-async-rxjava3`.
- Replace `org.apache.grails.events:grails-events-rxjava` or `grails-events-rxjava2` with `grails-events-rxjava3`.
- The `Promise` and `EventBus` APIs are the same for every implementation, so an application that only uses the Grails APIs needs no code change. The RxJava 3 promise factory carries a `null` value as the RxJava 1 one does: `createPromise()` resolves to `null`, a task that returns `null` resolves to `null`, and a `null` keeps its place in the list `onComplete` and `waitAll` deliver. The RxJava 2 factory does not: it fails with a `NullPointerException` or resolves to the `java.lang.Void` class instead.
- Code that hands RxJava types to the Grails ones, such as the `rx.Scheduler` given to the `RxEventBus` constructor, moves to the RxJava 3 types as described above.
- Configure RxJava 3 scheduling through `io.reactivex.rxjava3.plugins.RxJavaPlugins` instead of RxJava 1 `RxJavaHooks` or RxJava 2 `RxJavaPlugins`.

## Verification Checklist

- [ ] `gradle.properties` names the Grails 9 version and the wrapper is Gradle 9.8 or later
- [ ] No source file imports `rx.` or `io.reactivex.` without the `rxjava3` segment
- [ ] No test calls `toBlocking()`
- [ ] No build file declares `io.reactivex:rxjava`, `io.reactivex.rxjava2:rxjava`, `grails-async-rxjava`, `grails-async-rxjava2`, `grails-events-rxjava` or `grails-events-rxjava2`
- [ ] Every `Observer` implementation has `onSubscribe(Disposable)` and `onComplete()`
- [ ] Every `switchMap` and `flatMap` function returns an `ObservableSource`, never `null`
- [ ] No build file declares `grails-async-gpars` or `grails-events-gpars`, and no source file imports `groovyx.gpars`
- [ ] No source file calls `GParsPool.withPool` or `GParsConfig`
- [ ] No build file sets `grails { indy = true }`, and build logic reads `grails.indy` with `getOrElse` or `present`
- [ ] An application that sets `indy = false` declares `org.apache.groovy:groovy-callsite` for every source set that needs it
- [ ] `grails.converters.json.legacy` is set explicitly, and the JSON tests pass with the chosen value
- [ ] The application starts with no `deprecated` warning from the Grails async or events modules
- [ ] The full test suite passes on Groovy 6

## Pitfalls to Avoid

- Do not add `io.reactivex:rxjava` back to make old imports compile; port the code instead. Two RxJava generations on one classpath hide the mismatch until a datastore or plugin hands the application the wrong `Observable`.
- Do not compare a blocking result with `null` to detect an empty result; RxJava 3 throws instead.
- Do not keep a `Subscription` field for an RxGORM subscription; the handle is the `Disposable` passed to `onSubscribe`.
- Do not pin Groovy, Spock or RxJava versions in the application build; the Grails BOM manages them.
- Do not add the Groovy concurrency module alongside the GPars one to migrate gradually; only one implementation of each contract is used, and the event bus builder fails when it finds two.
- Do not set `grails { indy = false }` to avoid porting code; it needs `groovy-callsite` on every source set and rules out a GraalVM native image.
- Do not leave `grails.converters.json.legacy` unset; the default changes in a later release, so an application that has not checked its JSON with `false` will change output without a code change.
