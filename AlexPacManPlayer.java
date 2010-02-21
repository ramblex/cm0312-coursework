package player;

import java.util.List;
import java.util.Set;

import util.*;
import pacman.*;

public class AlexPacManPlayer implements PacManPlayer
{
    private final double MAX_DEPTH = 2;
    // Penalty to apply to a move if its the opposite of the previous move
    // - determined through experimentation
    private final double OPPOSITE_MOVE_PENALTY = 10;
    private Move last_move = Move.NONE;

    public Move chooseMove(Game game)
    {
        State current = game.getCurrentState();
        Move best_move = Move.NONE;
        double best_move_value = Double.NEGATIVE_INFINITY;
        for (Move move : game.getLegalPacManMoves())
        {
            List<State> successors = Game.getProjectedStates(current, move);
            //            try {Thread.sleep(5000);} catch(Exception e){}
            double move_value = min_value(successors, MAX_DEPTH);
            if (last_move == move.getOpposite())
                move_value -= OPPOSITE_MOVE_PENALTY;
            //            System.out.println(move+" => "+move_value);
            if (move_value > best_move_value)
            {
                best_move = move;
                best_move_value = move_value;
            }
        }
        System.out.println("Dots left: "+current.getDotLocations().size()+"\r");
        //        System.out.println("Best Move: "+best_move);
        last_move = best_move;
        return best_move;
    }

    /**
     * Find the best pacman move from the given state
     */
    private double max_value(State state, double depth)
    {
        if (Game.isFinal(state) || depth < 1)
            return evaluate(state);

        double v = Double.NEGATIVE_INFINITY;
        List<Move> moves = Game.getLegalPacManMoves(state);
        for (Move move : moves)
        {
            List<State> successors = Game.getProjectedStates(state, move);
            v = Math.max(min_value(successors, depth - 1), v);
        }
        return v;
    }

    /**
     * Find the best combined ghost move from the given state (i.e. the worst
     * move for pacman).
     */
    private double min_value(List<State> successors, double depth)
    {
        double v = Double.POSITIVE_INFINITY;
        for (State state : successors)
        {
            // Process the first of the successors
            if (Game.isFinal(state) || depth < 1)
                return evaluate(state);

            Set<List<Move>> combined = Game.getLegalCombinedGhostMoves(state);
            for (List<Move> move : combined)
            {
                State successor = Game.getNextState(state, move);
                v = Math.min(v, max_value(successor, depth - 1));
            }
        }
        return v;
    }

    // Utility function
    private double evaluate(State state)
    {
        if (Game.isLosing(state))
            return -1000;

        if (Game.isWinning(state))
            return 1000;

        double score = 0;
        score -= state.getDotLocations().size() * 2; // Most important
        // A problem arises with min distance when there are only a few dots
        // left and those dots are far apart. By gobbling a dot, the min
        // distance would become much larger so pacman avoids this and doesn't
        // gobble the dots.
        double min_dist = min_distance(state.getPacManLocation(),
                                       state.getDotLocations());
        score -= min_dist;
        return score;
    }

    private double min_distance(Location p, LocationSet dot_locations)
    {
        double min_dist = Double.POSITIVE_INFINITY;
        for (Location l : dot_locations)
        {
            double dist = Location.euclideanDistance(p, l);
            if (dist < min_dist)
                min_dist = dist;
        }
        return min_dist;
    }

    private double num_distinct_groups(Location p, Location dot_locations)
    {
        return 1;
    }

    private static String fillString(char fillChar, int count){
        // creates a string of 'x' repeating characters
        char[] chars = new char[count];
        while (count>0) chars[--count] = fillChar;
        return new String(chars);
    }
}