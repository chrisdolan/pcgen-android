PCGen Viewer for Android
========================

Goal: To create an Android app that’s usable at the game table for
players with PCGen electronic versions of their characters.

Intended features:
 * open character in app
 * render character
 * use the same PCGen core and data files as the desktop app
 * allow user to apply and remove conditions, like barbarian rage

I started this project in Jan 2013. Right now, it's not at all usable:
it can't even open a .pcg file yet. But it can read in most of the
LST files and game modes.

To use this code, you need my modifications to the PCGen source. Those
modifications are mostly refactoring out java.awt dependencies, which
don't exist on Android.  My changes are at
https://github.com/chrisdolan/pcgen-svn

I am releasing this code under the same license as PCGen. That is,
LGPL for the code and OGL for any data files that I might create (of
which there are none right now).
