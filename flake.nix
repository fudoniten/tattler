{
  description = "Tattler Notification Listener";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-22.05";
    utils.url = "github:numtide/flake-utils";
    helpers = {
      url = "git+https://git.fudo.org/fudo-public/nix-helpers.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, ... }:
    utils.lib.eachDefaultSystem (system:
      let pkgs = nixpkgs.legacyPackages."${system}";
      in {
        packages = rec {
          default = tattler;
          tattler = helpers.packages."${system}".mkClojureBin {
            name = "org.fudo/tattler";
            primaryNamespace = "tattler.cli";
            src = ./.;
          };
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ updateClojureDeps ];
          };
          tattler = pkgs.mkShell {
            buildInputs = with self.packages."${system}"; [ tattler-server ];
          };
        };
      }) // {
        nixosModules = rec {
          default = tattler;
          tattler = import ./module.nix self.packages;
        };
      };

}
