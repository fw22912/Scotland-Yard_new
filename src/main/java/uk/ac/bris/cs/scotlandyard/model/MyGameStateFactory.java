package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives
		) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.moves = ImmutableSet.of();

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if (mrX == null) throw new NullPointerException("mrX is empty!");
			if (mrX.isDetective()) throw new IllegalArgumentException("mrX is detective!");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Empty Graph");
			Set<Player> setPlayer = new HashSet<>();
			Set<Integer> setLocation = new HashSet<>();
			for (Player detective : detectives) {
//				if(detective == null) throw new NullPointerException("detectives are empty!");
				if (!setPlayer.add(detective)) throw new IllegalArgumentException("Duplicate Detective");
				if (!setLocation.add(detective.location()))
					throw new IllegalArgumentException("Overlap Detective Location");
				if (detective.isMrX()) throw new IllegalArgumentException("There is more than one mrX");
				if (detective.has(ScotlandYard.Ticket.SECRET))
					throw new IllegalArgumentException("Detective has Secret Ticket");
				if (detective.has(ScotlandYard.Ticket.DOUBLE))
					throw new IllegalArgumentException("Detective has Double Ticket");
			}
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> newPiece = new HashSet<>();
			newPiece.add(mrX.piece());
			for (Player playerDetective : detectives) {
				newPiece.add(playerDetective.piece());
			}
			return ImmutableSet.copyOf(newPiece);
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player playerDetective : detectives) {
				if (playerDetective.piece().equals(detective)) return Optional.of(playerDetective.location());
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (piece.isMrX()) return Optional.of(ticket -> mrX.tickets().get(ticket));
			else if (piece.isDetective())
				for (Player playerDetective : detectives) {
					if (playerDetective.piece().equals(piece))
						return Optional.of(ticket -> playerDetective.tickets().get(ticket));
				}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			if (winner != null) return winner;
			else return ImmutableSet.of();
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			Set<Move.SingleMove> availableMoves = new HashSet<>();
			Set<Integer> detectiveOccupied = new HashSet<>();
			for (Player detective : detectives) {
				detectiveOccupied.add(detective.location());
			}

			for (int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				if (!detectiveOccupied.contains(destination)) {
					for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						// TODO find out if the player has the required tickets
						//  if it does, construct a SingleMove and add it the collection of moves to return

						if (player.has(t.requiredTicket())) {
							Move.SingleMove newMoves = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
							availableMoves.add(newMoves);
						}
					}
					// TODO consider the rules of secret moves here
					//  add moves to the destination via a secret ticket if there are any left with the player
					if (player.has(ScotlandYard.Ticket.SECRET) && player.isMrX()) {
						Move.SingleMove newMoves = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
						availableMoves.add(newMoves);
					}
				}
			}

			// TODO return the collection of moves
			return availableMoves;
		}

		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log) {
			Set<Move.DoubleMove> doubleMoves = new HashSet<>();
			//storing firstMoves
			Set<Move.SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);
			Integer availableMoves = setup.moves.size() - log.size();
			if (player.has(ScotlandYard.Ticket.DOUBLE) && availableMoves >= 2) {
				for (Move.SingleMove firstMove : firstMoves) {
					Set<Move.SingleMove> secondMoves = makeSingleMoves(setup, detectives, player, firstMove.destination);
					for (Move.SingleMove secondMove : secondMoves) {
						if ((player.hasAtLeast(firstMove.ticket, 2)
								&& firstMove.ticket == secondMove.ticket)
								|| firstMove.ticket != secondMove.ticket) {
							Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, firstMove.ticket, firstMove.destination, secondMove.ticket, secondMove.destination);
							doubleMoves.add(doubleMove);
						}
					}
				}
			}
			return doubleMoves;
		}


		@Nonnull
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> newMoves = new HashSet<>();

			if (this.remaining.contains(mrX.piece())) {
				//How to make single moves
				Set<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectives, mrX, mrX.location());
				Set<Move.DoubleMove> doubleMoves = makeDoubleMoves(setup, detectives, mrX, mrX.location(), log);
				newMoves.addAll(singleMoves);
				newMoves.addAll(doubleMoves);
			} else {
				for (Player playerDetective : detectives) {
					if (this.remaining.contains(playerDetective.piece())) {
						Set<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectives, playerDetective, playerDetective.location());
						newMoves.addAll(singleMoves);
					}
				}
			}
			moves = ImmutableSet.copyOf(newMoves);
			return moves;
		}

		@Nonnull
		private ScotlandYard.Ticket usedTicket(Player player){

			return null;
		}

		@Nonnull
		private Player updatePlayer(Player player){
			if (player.isDetective()){

			}
			else{
				player = player.use(usedTicket(player));
			}
			return null;
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			else {

			}
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
	}

	@Nonnull @Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {

		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
