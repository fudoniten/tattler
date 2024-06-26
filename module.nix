packages:

{ config, lib, pkgs, ... }:

with lib;
let
  tattler = packages."${pkgs.system}".tattler;
  cfg = config.services.tattler;

in {
  options.services.tattler = with types; {
    enable = mkEnableOption "Enable Tattler notification listener.";

    verbose = mkEnableOption "Generate verbose logs and output.";

    notification-topic = mkOption {
      type = str;
      description = "MQTT topic on which to send notifications.";
    };

    mqtt = {
      host = mkOption {
        type = str;
        description = "Hostname of the MQTT server.";
      };

      port = mkOption {
        type = port;
        description = "Port on which the MQTT server is listening.";
        default = 1883;
      };

      user = mkOption {
        type = nullOr str;
        description = "User as which to connect to the MQTT server.";
        default = null;
      };

      password-file = mkOption {
        type = nullOr str;
        description =
          "User password file with which to connect to the MQTT server.";
        default = null;
      };
    };
  };

  config = mkIf cfg.enable {
    systemd.user.services.tattler = {
      path = [ tattler ];
      wantedBy = [ "graphical-session.target" ];
      after = [ "graphical-session.target" ];
      serviceConfig = {
        ExecStart = pkgs.writeShellScript "tattler.sh" (concatStringsSep " " ([
          "tattler"
          "--mqtt-host=${cfg.mqtt.host}"
          "--mqtt-port=${toString cfg.mqtt.port}"
          "--notification-topic=${cfg.notification-topic}"
          "--app-name=tattler"
        ] ++ (optional cfg.verbose "--verbose")
          ++ (optional (cfg.mqtt.user != null) "--mqtt-user=${cfg.mqtt.user}")
          ++ (optional (cfg.mqtt.password-file != null)
            "--mqtt-password=${cfg.password-file}")));
        Restart = "always";
      };
    };
  };
}
