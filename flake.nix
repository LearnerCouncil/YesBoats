{
  description = "Flake to build and develop YesBoats";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-24.05";
  };

  outputs = { self, nixpkgs, ... } @inputs:
    let
      forAllSystems = function:
        nixpkgs.lib.genAttrs [
          "x86_64-linux"
          "aarch64-linux"
          "x86_64-darwin"
          "aarch64-darwin"
        ]
          (system: function (import nixpkgs { inherit system; }));
    in
    {
      devShells = forAllSystems (pkgs: {
        default = pkgs.mkShell {
          packages = with pkgs; [
            maven
            jdk17_headless
          ];
        };
      });

      packages = forAllSystems (pkgs: rec {
        default = yesboats;
        yesboats = pkgs.callPackage
          ({ lib, maven, jdk17_headless }: maven.buildMavenPackage rec {
            pname = "YesBoats";
            version = "1.5.2";

            src = ./.;

            mvnHash = "sha256-EnSZWFqYUHvfqE8Sbr/BFEXsRoyyf3gb0JPJZVsB8zw=";

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
          })
          { };
      });
    };
}
