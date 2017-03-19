![Logo of the project](https://url.toproject.com/path/to/logo.png)

# scheduler2ics
> Assign jobs to workers and write the information who is doing when which job as ics file

The project was intended to have jobs, which must be assigned to a group of people. This will be done by
round robin.
The jobs can be configured, when they have to be done.
If the workers are on holiday on their specific time they won't re-work the missing work. This is done
because we do not struggle the work order of the people.

## Features

* see cli below
* caution the csv files are expected to always start with a headline, this first line is always ignored!

## Getting started

TODO

```shell
Usage: <hiringProcess class> [options]
  Options:
    --help, -h

      Default: false
  * --holidays, -hs
      File for Holidays <dd.MM.yyyy,dd.MM.yyyy,event>
  * --jobDescriptions, -js
      File for Jobs <name,dayOfWeek (mo=1,...,sun=7),duration,begin,end>
  * --workers, -ws
      File for Workers <Name,JobA JobB..,Holidays>
    -log, -verbose
      Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]
      Default: <empty string>
```

Here you should say what actually happens when you execute the code above.


## Basic Usage

Following commands must/can be added to cli. holidays, jobDescriptions, workers is required

```bash
Usage: <hiringProcess class> [options]
  Options:
    --help, -h

      Default: false
  * --holidays, -hs
      File for Holidays <dd.MM.yyyy,dd.MM.yyyy,event>
  * --jobDescriptions, -js
      File for Jobs <name,dayOfWeek (mo=1,...,sun=7),duration,begin,end>
  * --workers, -ws
      File for Workers <Name,JobA JobB..,Holidays>
    -log, -verbose
      Level of verbosity [ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF]
      Default: <empty string>
```

## Developing

Here's a brief intro about what a developer must do in order to start developing
the project further:

```shell
git clone https://github.com/your/awesome-project.git
cd awesome-project/
packagemanager install
```

And state what happens step-by-step.

### Testing

Here you should state how to run the whole test suite for the project.

### Building

If your project needs some additional steps for the developer to build the
project after some code changes, state them here:

```shell
./configure
make
make install
```
Here again you should state what actually happens when the code above gets
executed.

### Deploying / Publishing

In case there's some step you have to take that publishes this project to a
server, this is the right time to state it.

```shell
packagemanager deploy awesome-project -s server.com -u username -p password
```

And again you'd need to tell what the previous code actually does.

## Contributing

When you publish something open source, one of the greatest motivations is that
anyone can just jump in and start contributing to your project.

These paragraphs are meant to welcome those kind souls to feel that they are
needed. You should state something like:

"If you'd like to contribute, please fork the repository and use a feature
branch. Pull requests are warmly welcome."

If there's anything else the developer needs to know (e.g. the code style
guide), you should link it here. If there's a lot of things to take into
consideration, it is common to separate this section to its own file called
`CONTRIBUTING.md` (or similar). If so, you should say that it exists here.

## Post processing

https://www.w3.org/Tools/Ical2html/

## Licensing

see LICENSE file in repository
