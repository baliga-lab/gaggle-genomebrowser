## Genome Browser Architecture

Note: There are a few examples of experimental software design in the code - some not
entirely successful. The app package is most in need of cleanup. The current version of
the program derives from a very quick and dirty prototype, some of that code remains
unreformed.

### Components

The application is divided into components which communicate by events. The app package
defines classes at application scope which coordinate loading and configuring of
components and queuing of events.

* search
* visualization
* gaggle
* sqlite


package structure and dependencies (org.systemsbiology.<package>):
ncbi:
ucscgb:
util:
genomebrowser:


### Component / plug-in architecture

There is an intention of creating a plug-in architecture for the genome browser to allow
extensibility. Implementation is very rudimentary and incomplete. In the app package, see
classes ExternalAPI, Plugin, and PluginManager. The hope is to allow plug-ins to add UI
elements (menus, etc.) and receive events from the Application. The Gaggle interface code
is the test-case for the plug-in functionality.

It might be wise to look into OSGI or Spring to support plug-ins rather that go too far
in implementing my own.

HackySearchEngine is intended to be replaced by Lucene or SQL.

The in-memory model of a dataset existed first. The Sqlite datastore came later. So,
watch out for some inconsistency on which is considered the canonical representation of a
dataset. Resolving that is still in progress.

### Design patterns:

Features are often implemented as flyweights. The Flyweight design pattern provides an
object-oriented interface to numerical data efficiently stored in arrays. We thus avoid
the overhead of object creation for millions of data points and preserving data locality
while hiding details of data access from renderers and other consumers of track data.

Several patterns described by Heer and Agrawala in Software Design Patterns for
Information Visualization have informed our architecture. The overall structure of the
application loosely conforms to the Reference Model design pattern, a specialization of
Model-View-Controller which further divides the model into a visualization and underlying
data.  Several other components of the GB can be seen in light of patterns from this
collection including Renderer, Data Column and Scheduler. 

