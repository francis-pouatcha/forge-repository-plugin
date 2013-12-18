forge-repository-plugin
=======================

Forge plugin for the generation of repository design pattern classes based on apache deltaspike data.

This plugin is not yet uploaded to the forge plugin repository. In order to use this plugin:

1. clone the plugin project
2. start forge
3. install the plugin using: forge source-plugin $PATH_TO_PTOJECT/forge-source-plugin
3. create a project containing JPA entity beans
4. $ repo setup
5. $ repo new-repository package.JPAEntityBean
6. Go ahead an use the repository to access you entity database.
