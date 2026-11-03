# Create: Pneumatics

A Create addon that runs on air. NeoForge 1.21.1, Create 6.0.10 or newer.

A Roots Blower turns rotation into pressure. Brass tubes carry it, pushing out of one flange and pulling into the other, and pressure falls off the further a tube is from the blower. Leave a tube end open and it blows (or sucks) like a fan, fan processing included.

- **Pressure Gauge** reads a tube, gives a comparator signal and works with Display Links.
- **Relief Valve** dumps a whole network on a click or redstone.
- **Bell Jar** sits on a basin, gets pumped down by a tube pulling vacuum, and runs vacuum recipes on what's underneath. Stack jars for a bigger chamber that pumps down slower. A batch you started keeps going on a contraption as long as there's a Vacuum Tank on board.
- **Vacuum Suction Pad** lifts the block in front of it when its contraption sets off (or whatever it runs into) and puts it down when it stops, contents and all. Runs off a **Vacuum Tank** on the contraption, topped up at a **Pneumatic Interface**. Mechanical Arms can work a pad too.
- **Pneumatic Terminal** shoots items down the tubes. They ride toward lower pressure and drop into the terminal at the end of the line. An **Air Diverter** sends filtered items down its branch.

JEI is optional and shows the vacuum recipes.

Placeholder models for now.

## License

Code is MIT, see `LICENSE`. Textures, models and the rest of `assets/` are All Rights Reserved, see `LICENSE-ASSETS`. Modpacks can ship the jar.
