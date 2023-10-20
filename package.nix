{
  mkSbtDerivation,
  jre,
  runtimeShell,
}:
mkSbtDerivation {
  pname = "dfa";
  version = "0.0.1";
  src = ./.;
  depsSha256 = "sha256-kamygSv8zIeeXGWXzU+8rg23MdOWwhRv2KvbP0tgRXg=";

  buildPhase = ''
    sbt assembly
  '';

  installPhase = ''
    mkdir -p $out/share/dfa
    cp -vfL target/scala-*/dfa-*.jar $out/share/dfa.jar

    mkdir -p $out/bin
    tee $out/bin/dfa <<EOF
    #${runtimeShell}
    ${jre}/bin/java -jar $out/share/dfa.jar
    EOF
    chmod +x $out/bin/*
  '';
}
