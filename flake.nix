{
  description = "Flake to build and develop YesBoats";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-25.05";
    flake-parts = {
      url = "github:hercules-ci/flake-parts";
      inputs.nixpkgs-lib.follows = "nixpkgs";
    };
  };

  outputs =
    { flake-parts, ... }@inputs:
    flake-parts.lib.mkFlake { inherit inputs; } (
      { ... }:
      {
        systems = [
          "x86_64-linux"
          "aarch64-linux"
          "x86_64-darwin"
          "aarch64-darwin"
        ];

        perSystem =
          { config, pkgs, ... }:
          {
            formatter = pkgs.nixfmt-tree;

            devShells.default = pkgs.mkShell {
              packages = with pkgs; [
                maven
                jdk17
              ];
            };

            packages = {
              default = config.packages.yesboats;
              yesboats = pkgs.callPackage (
                {
                  lib,
                  maven,
                  jdk17_headless,
                }:
                let
                  jdk' = jdk17_headless;
                in
                maven.buildMavenPackage rec {
                  pname = "YesBoats";
                  version =
                    with builtins;
                    elemAt (match ".*${pname}</artifactId>[[:s:]]*<version>([[:d:]]{1,2}\.[[:d:]]{1,2}\.[[:d:]]{1,2}).*" (readFile ./pom.xml)) 0;

                  src = ./.;

                  buildOffline = true;
                  mvnHash = "sha256-V641/XNFF8PdimmceSyp4JC0K6JzvMEsqw/v24kUw3E=";

                  mvnJdk = jdk';
                  nativeBuildInputs = [
                    jdk'
                  ];

                  installPhase = ''
                    mkdir -p $out/plugins/
                    install -Dm644 target/${pname}-${version}.jar $out/plugins/
                  '';

                  meta = {
                    description = "An ice boat racing plugin";
                    homepage = "https://github.com/LearnerCouncil/YesBoats";
                    license = lib.licenses.gpl3;
                  };
                }
              ) { };
            };
          };
      }
    );
}
