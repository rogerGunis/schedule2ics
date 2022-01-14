define geschwistertag

	sed -i -e '0,/.*summary.*Geschwistertag$(1)/s#Geschwistertag$(1)#G::$(2)#' /var/tmp/schedule/allEvents.html

endef

prepare:
	mkdir -p /var/tmp/schedule/

compile:
	export GRADLE_HOME=$$HOME/.gradle
	./gradlew build 
	./gradlew :jobService:test

rmIgnore: 
	sed -i -e 's#^\(\s*\)@Ignore#\1//@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java

setIgnore:
	sed -i -e 's#//@Ignore#@Ignore#' jobService/src/test/java/de/gunis/roger/jobService/WithPostProcessing.java

rmPdf:
	test -f /var/tmp/schedule/allEvents.pdf && mv /var/tmp/schedule/allEvents.pdf /var/tmp/schedule/allEvents.previous.pdf || true

gt:
	./gt.sh

geschwisterTag:
	$(call geschwistertag,1,Sarah+Michi)
	$(call geschwistertag,1,Nina+Moritz)
	$(call geschwistertag,1,Carina+Jürgen)

	$(call geschwistertag,1,Sarah+Michi)
	$(call geschwistertag,1,Nina+Moritz)
	$(call geschwistertag,1,Carina+Jürgen)

	$(call geschwistertag,1,Sarah+Michi)
	$(call geschwistertag,1,Nina+Moritz)
	$(call geschwistertag,1,Carina+Jürgen)

	$(call geschwistertag,1,Sarah+Michi)
	$(call geschwistertag,1,Nina+Moritz)
	$(call geschwistertag,1,Carina+Jürgen)

	! egrep ".*summary.*Geschwistertag[1-2]" /var/tmp/schedule/allEvents.html

createPdf:
	bash /var/tmp/schedule/allEvents.sh
	
viewPdf:
	pdftk /var/tmp/schedule/allEvents.pdf  cat 3-end output /var/tmp/schedule/allEvents.cut.pdf
	
pdfonly: compile build2 rmPdf geschwisterTag createPdf

pdf: compile build2 rmPdf geschwisterTag createPdf view

render:
	google-chrome-stable --headless --disable-gpu --print-to-pdf=/var/tmp/schedule/allEvents.pdf file:///var/tmp/schedule/allEvents.html && qpdfview /var/tmp/schedule/allEvents.pdf

view:
	test -f /var/tmp/schedule/allEvents.pdf && qpdfview /var/tmp/schedule/allEvents.pdf

mv:
	mv /var/tmp/schedule/allEvents.pdf $(HOME)/windows/kochplan.pdf

build2:
	java -jar $$(find . -name "jobService.jar") --workers $$(find jobService/src/main -name "Workers.csv") --outputFilePath /var/tmp/schedule/ \
	--jobDescriptions ./jobService/src/main/resources/WithPostProcessing/inputData/JobDescription.csv \
  --holidays ./jobService/src/main/resources/WithPostProcessing/inputData/Holidays.csv -log ALL --withPostprocessing

travis: compile build2 geschwisterTag createPdf
	zip -r postProcess.zip /var/tmp/schedule
	mkdir -p .store
	java -jar -DACCOUNT_USER=$$ACCOUNT_USER $$(find . -name "icalToGoogleDeployment*.jar") \
	--icsDirectory /var/tmp/schedule \
	--apiKey icalToGoogleDeployment/src/test/resources/client_secrets_apiKey.json \
	-log INFO

clean: 
	rm -f rm jobService/build/libs/jobService.jar icalToGoogleDeployment/build/libs/icalToGoogleDeployment.jar
	rm -f build/libs/schedule2ics.jar
	mkdir -p /var/tmp/schedule/
	rm /var/tmp/schedule/*
	// ./gradlew clean

