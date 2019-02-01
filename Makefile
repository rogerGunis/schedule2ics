pdf: 
	sed -i -e 's#@Ignore#//@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	./gradlew build 
	./gradlew :jobService:test --tests "de.gunis.roger.jobService.WithPostProcessing.withPostprocessing"
	sed -i -e 's#//@Ignore#@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
