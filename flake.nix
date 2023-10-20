{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    sbt = {
      url = "github:zaninime/sbt-derivation";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    flake-parts = {
      url = "github:hercules-ci/flake-parts";
    };
  };

  outputs = inputs @ {
    self,
    nixpkgs,
    sbt,
    flake-parts,
  }:
    flake-parts.lib.mkFlake {inherit inputs;} {
      systems = ["x86_64-linux" "aarch64-linux"];

      perSystem = {
        pkgs,
        system,
        ...
      }: {
        _module.args.pkgs = import nixpkgs {
          inherit system;
          overlays = [sbt.overlays.default];
        };

        packages.default = with pkgs;
          callPackage ./package.nix {
            jre = jre_headless;
          };

        devShells.default = with pkgs;
          mkShell.override {stdenv = stdenvNoCC;} {
            packages = [
              scala
              sbt-extras
            ];
          };
      };
    };
}
