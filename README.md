# Mini-C Compiler

_A project by Maxime Segura and Joe Khawand._

## Introduction

This project aims to create a compiler for a small portion of the c language called mini-c.

The project is divided into 6 parts:

- Typing
- Generating RTL code
- Generating ERTL code and checking liveness
- Liveness Analysis
- Generating LTL code
- Generating assembly code

_This mardown file will quickly present the difficulties faced throughout the implementation._



## Setup

We firstly encountered a lot of problems in setting up the project. We used the [Vscode Extension Pack for java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack). The main problem was that the extension was not recognising the mini_c folder as a java package. We resolved it by altering the extension settings and recreating the whole project.

## Typing

The main difficulty encountered in typing was finding a way to structure the file to enable good storage and organisation. For that we added linkedlists and hashmaps to the class to store the types of the functions, the structures, and the variables.

## Generating RTL

The main issue we encountered here was the implementation of the two functions _visit(Eaccess)_ and _visit(Eassign)_. We had trouble finding a way to store the entry variables to be able to access the corresponding registers. To solve that we decided to add a register member to the class decl_var. Our implementation gives out an almost perfect compilation but the corresponding output is not correct. We can't seem to figure out the problem.

## Generating ERTL

The main problem here is that we inherited the issues of our RTL implementation, so the code behavior was totally wrong.

## Liveness 

For the liveness we changed the field _next_ in the _Register.class_ to be able to access it from _Liveness.class_. The rest was a straitforward implmentation from the course.

## Generating LTL
In this part we only implemented the Interference Graph. We constructed the differents functions to build the preference and interference egdes. 

## Conclusion

In conclusion we were able to accomplish some important steps in creating a mini-c compiler. But due to the many problems we faced and the limited time on our hands we were not able to fully complete the task at hand. Nonetheless we experienced first hand the difficulties of creating a compiler and learned to appreciate the complexity of the problem.
