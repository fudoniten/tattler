{
  description = "Tattler Notification Listener";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    utils.url = "github:numtide/flake-utils";
    helpers = {
      url = "github:fudoniten/fudo-nix-helpers";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    notifierClj = {
      url = "github:fudoniten/clojure-notifier";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, notifierClj, ... }:
    utils.lib.eachDefaultSystem (system:
      let
        inherit (helpers.packages."${system}") mkClojureBin;
        pkgs = nixpkgs.legacyPackages."${system}";
        cljLibs = {
          "org.fudo/notifier" = "${notifierClj.packages."${system}".notifier}";
        };

      in {
        packages = rec {
          default = tattler;
          tattler = mkClojureBin {
            name = "org.fudo/tattler";
            primaryNamespace = "tattler.cli";
            src = ./.;
            inherit cljLibs;
          };
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ (updateClojureDeps cljLibs) ];
          };
          tattler = pkgs.mkShell {
            buildInputs = [ self.packages."${system}".tattler ];
          };
        };
      }) // {
        nixosModules = rec {
          default = tattler;
          tattler = import ./module.nix self.packages;
        };
      };

}
