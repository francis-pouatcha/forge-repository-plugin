forge-repository-plugin
=======================

Forge plugin for the generation of repository design pattern classes based on apache deltaspike data.

This plugin is not yet uploaded to the forge plugin repository. In order to use this plugin:

1. clone the plugin project
2. start forge

2a. install the plugin using: forge source-plugin $PATH_TO_PTOJECT/forge-source-plugin

3. create a project containing JPA entity beans
4. forge> new-project --named adspc --topLevelPackage org.adorsys.adspc --finalName adspc
5. forge> as7 setup
7. forge> persistence setup --provider HIBERNATE --container JBOSS_AS7
8. forge> entity --named Site --package ~.jpa --idStrategy AUTO
9. forge> field string --named displayName
10. forge> reporest setup --activatorType APP_CLASS
11. forge> reporest endpoint-from-entity --jpaPackage src/main/java/org/adorsys/adspc/
12. forge> repotest setup
13. forge> repotest create-test --packages src/main/java/org/adorsys/adspc/
14. forge> as7 start
15. forge> mvn clean install -P arq-jboss_as_remote_7.x

