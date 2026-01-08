# My minimal reproduceable examples for issues in various libraries.

Each individual example is layout in different branches, so you can check out the branch that corresponds to the issue only you are interested in.

```sh
git clone https://github.com/lqhuang/minimal-reproducible-issues --single-branch --branch <branchname>
```

- `staging-with-case-class-and-circe`: for [circe/circe PR#2083](https://github.com/circe/circe/pull/2083)
- `nested-objects-interop-between-scala-java`
- `scala-native-java-stream`: for [scala-native/scala-native Issue#4742](https://github.com/scala-native/scala-native/issues/4742)

Create new orphan branch to add new example:

```sh
git checkout --orphan <branchname>
git reset HEAD -- # to unstage all files
git clean -n # to see what will be removed
#git clean -f # DANGEROUS: to remove all files
```

or

```
./new-orphan.sh <branchname>
```
