# ark-tools
Tools to work with the binary files of ark. Extract information, manipulate them or write them from scratch.

## HowTo build
```
# Debian Buster
#----------------
apt-get install maven openjdk-11-jre openjdk-11-jdk git
git clone https://github.com/Qowyn/ark-savegame-toolkit.git
git clone https://github.com/McBane87/ark-tools.git
#
export MAVEN_OPTS='-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true'
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
#
cd ark-savegame-toolkit
mvn install
#
cd ark-tools
mvn install
```
