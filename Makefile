define geschwistertag

	sed -i -e '0,/.*summary.*Geschwistertag$(1)/s#Geschwistertag$(1)#G::$(2)#' /var/tmp/schedule/allEvents.html

endef

prepare:
	mkdir -p /var/tmp/schedule/

pdf: 
	test -f /var/tmp/schedule/allEvents.pdf && mv /var/tmp/schedule/allEvents.pdf /var/tmp/schedule/allEvents.previous.pdf || true
	sed -i -e 's#^\(\s*\)@Ignore#\1//@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	export GRADLE_HOME=$$HOME/.gradle
	./gradlew build 
	./gradlew :jobService:test --tests "de.gunis.roger.jobService.WithPostProcessing.withPostprocessing"
	sed -i -e 's#//@Ignore#@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java
	
	$(call geschwistertag,1,Birgit+Raoul)
	$(call geschwistertag,1,Doris+Jerome)
	$(call geschwistertag,1,Lynn+Nick)
	$(call geschwistertag,1,Birgit+Raoul)
	$(call geschwistertag,1,Doris+Jerome)
	$(call geschwistertag,1,Lynn+Nick)
	$(call geschwistertag,1,Birgit+Raoul)
	$(call geschwistertag,1,Doris+Jerome)
	$(call geschwistertag,1,Lynn+Nick)
	$(call geschwistertag,1,Birgit+Raoul)
	$(call geschwistertag,1,Doris+Jerome)
	$(call geschwistertag,1,Lynn+Nick)
	
	! egrep ".*summary.*Geschwistertag[1-2]" /var/tmp/schedule/allEvents.html
	
	bash /var/tmp/schedule/allEvents.sh
	
	pdftk /var/tmp/schedule/allEvents.pdf  cat 3-end output /var/tmp/schedule/allEvents.cut.pdf
	test -f /var/tmp/schedule/allEvents.pdf && qpdfview /var/tmp/schedule/allEvents.pdf

render:
	google-chrome-stable --headless --disable-gpu --print-to-pdf=/var/tmp/schedule/allEvents.pdf file:///var/tmp/schedule/allEvents.html && qpdfview /var/tmp/schedule/allEvents.pdf

view:
	qpdfview /var/tmp/schedule/allEvents.pdf

mv:
	mv /var/tmp/schedule/allEvents.pdf $(HOME)/windows/kochplan.pdf


clean: 
	mkdir -p /var/tmp/schedule/
	rm /var/tmp/schedule/*
	// ./gradlew clean

