with import <nixpkgs> {};
mkShell.override {stdenv = stdenvNoCC;} {
  packages = [
    scala
    sbt-extras
  ];
}
