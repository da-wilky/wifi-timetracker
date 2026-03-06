rec {
  description = "WiFi TimeTracker";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";

  outputs = { self, nixpkgs, ... }@inputs:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.android_sdk.accept_license = true;
        config.allowUnfree = true;
      };

      androidComposition = pkgs.androidenv.composeAndroidPackages {
        cmdLineToolsVersion = "11.0";
        platformToolsVersion = "35.0.2";
        buildToolsVersions = [ "35.0.0" ];
        platformVersions = [ "35" ];
        includeEmulator = false;
        includeNDK = false;
      };

      androidSdk = androidComposition.androidsdk;

      zeroshot = pkgs.buildNpmPackage rec {
        pname = "zeroshot";
        version = "5.5.0";

        src = pkgs.fetchFromGitHub {
          owner = "covibes";
          repo = pname;
          rev = "v${version}";
          hash = "sha256-iZUJGsgRdD/rtH9CKLqYGEetfrmJENNI+v/GN8qL76Y=";
        };

        npmDepsHash = "sha256-p69N+NjbRXDC4//4xMOyQHdIrgjZjEiaON22c7VrKbI=";

        env.PUPPETEER_SKIP_DOWNLOAD = "1";

        postPatch = ''
          sed -i '/"postinstall"/d' package.json
        '';

        dontNpmBuild = true;

        nativeBuildInputs = [ pkgs.makeWrapper ];

        postInstall = ''
          wrapProgram $out/bin/zeroshot \
            --set ZEROSHOT_TUI_BINARY_SKIP "1"
        '';

        meta.mainProgram = "zeroshot";
      };

      apk = pkgs.stdenv.mkDerivation {
        pname = "wifi-timetracker";
        version = "1.0";

        src = ./.;

        nativeBuildInputs = with pkgs; [
          jdk17
          gradle
        ];

        ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
        ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
        JAVA_HOME = "${pkgs.jdk17}";
        GRADLE_USER_HOME = ".gradle-home";

        buildPhase = ''
          runHook preBuild

          export HOME=$(pwd)
          mkdir -p $GRADLE_USER_HOME

          gradle --no-daemon assembleRelease

          runHook postBuild
        '';

        installPhase = ''
          runHook preInstall

          mkdir -p $out
          cp app/build/outputs/apk/release/*.apk $out/

          runHook postInstall
        '';

        meta = {
          description = "WiFi TimeTracker Android App";
          platforms = [ "x86_64-linux" ];
        };
      };

    in {
      packages.x86_64-linux = {
        inherit apk;
        default = apk;
      };

      devShells.x86_64-linux.default = pkgs.mkShell {
        packages = with pkgs; [
          jdk17
          zeroshot
        ];

        ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
        ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";

        shellHook = ''
          echo
          echo
          echo
          echo -e "\033[1;31mWelcome to the ${description} environment!"
          echo -e "\033[0;32mVirtual environment activated with all dependencies installed."
        '';
      };
    };
}

