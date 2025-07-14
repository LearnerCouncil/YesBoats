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
                maven.buildMavenPackage rec {
                  pname = "YesBoats";
                  version =
                    with builtins;
                    elemAt (match ".*${pname}</artifactId>[[:s:]]*<version>([[:d:]]{1,2}\.[[:d:]]{1,2}\.[[:d:]]{1,2}).*" (readFile ./pom.xml)) 0;

                  src = ./.;

                  mvnHash = "sha256-wLsyvNLluSDCMynIW4R8Ym4Drtv728y0VaD+e/8UhrU=";

                  mvnJdk = jdk17_headless;
                  nativeBuildInputs = [
                    jdk17_headless
                  ];

                  installPhase = ''
                    mkdir -p $out/share/${pname}
                    install -Dm644 target/${pname}-${version}.jar $out/share/${pname}
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
