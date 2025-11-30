JAVAC = javac
JAVA = java
BIN = bin
FLAGS = -d $(BIN) -encoding UTF-8

# Liste des dossiers contenant les fichiers .java
SOURCES = main/*.java ihm/*.java metier/*.java modele/*.java donnees/*.java api/*.java

all: compile

compile:
	mkdir -p $(BIN)
	$(JAVAC) $(FLAGS) $(SOURCES)

run: compile
	$(JAVA) -cp $(BIN) main.MainApp

clean:
	rm -rf $(BIN)
