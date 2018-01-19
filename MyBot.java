import hlt.*;
import java.util.*;

public class MyBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Boss");

        // We now have 1 full minute to analyse the initial map.
        final String initialMapIntelligence =
                "width: " + gameMap.getWidth() +
                "; height: " + gameMap.getHeight() +
                "; players: " + gameMap.getAllPlayers().size() +
                "; planets: " + gameMap.getAllPlanets().size();
        Log.log(initialMapIntelligence);

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);
            int counter = 0;
            Map<Integer, Planet> planets = gameMap.getAllPlanets();
            for (Planet p : planets.values()) {
              if (p.isOwned()) {
                counter++;
              }
            }
            // If there are no more unowned planets, its time to fight!
            if (counter == planets.size()) {
              for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                UndockMove m = new UndockMove(ship);
                moveList.add(m);
                break;
              }
              Networking.sendMoves(moveList);
              continue;
            }

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }
                /* Finds undocked planets nearest to my ships */
              Map<Double, Entity> entities_by_distance = gameMap.nearbyEntitiesByDistance(ship);
              Map<Double, Entity> sorted_entities_by_distance = new TreeMap<Double, Entity>(entities_by_distance);
              for (Entity entity : sorted_entities_by_distance.values()) {
                  if (entity instanceof Planet) {
                    Planet planet = (Planet) entity;
                    /* Allows ships docked to owned planets to move to other locations */
                    if (planet.isOwned() || planet.isFull()) {
                      if (ship.getDockingStatus() == Ship.DockingStatus.Docked) {
                        UndockMove m = new UndockMove(ship);
                        moveList.add(m);
                        break;
                      }
                      continue;
                    }
                    if (ship.canDock(planet)) {
                        moveList.add(new DockMove(ship, planet));
                        break;
                    }

                    final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED/2);
                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                    }

                    break;
                  }
              }
            }
            Networking.sendMoves(moveList);
        }
    }
}
