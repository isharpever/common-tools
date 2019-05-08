mvn -N clean source:jar javadoc:jar deploy -Denforcer.skip=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -U
