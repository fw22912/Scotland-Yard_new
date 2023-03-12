package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.crypto.spec.PSource;
import javax.swing.text.html.Option;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.sun.javafx.geom.Edge;
import com.sun.media.jfxmedia.events.PlayerStateEvent;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState {
		//attribute -for constructor
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		//constructor
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.moves = ImmutableSet.of();

			//throwing exceptions
			if (mrX == null) throw new NullPointerException("testNullMrXShouldThrow: mrX is empty!");
			if (detectives == null)
				throw new NullPointerException("testNullDetectiveShouldThrow: Detectives are empty!");
			if (mrX.isDetective()) throw new IllegalArgumentException("testNoMrXShouldThrow: MrX is a detective!");
			Set<Player> set = new HashSet<>();
			Set<Integer> setLocation = new HashSet<>();
			for (Player playerDetective : detectives) {
				if(playerDetective == null) throw new NullPointerException("testAnyNullDetectiveShouldThrow: One of the detectives is null");
				if (playerDetective.isMrX())
					throw new IllegalArgumentException("testMoreThanOneMrXShouldThrow: There is more than one MrX!");
				if (playerDetective.has(ScotlandYard.Ticket.DOUBLE))
					throw new IllegalArgumentException("testDetectiveHaveDoubleTicketShouldThrow: Detectives have double tickets!");
				if (playerDetective.has(ScotlandYard.Ticket.SECRET))
					throw new IllegalArgumentException("testDetectiveHaveSecretTicketShouldThrow: Detectives have secret tickets!");
				if (!set.add(playerDetective))
					throw new IllegalArgumentException("testDuplicateDetectivesShouldThrow: Duplicate detectives!");
				if (!setLocation.add(playerDetective.location()))
					throw new IllegalArgumentException("testLocationOverlapBetweenDetectivesShouldThrow");
			}
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("testEmptyMovesShouldThrow: Moves is empty!");
			if (setup.graph.nodes().isEmpty())
				throw new IllegalArgumentException("testEmptyGraphShouldThrow: Empty graph!");
		}

		//implementing methods
		@Nonnull
		public GameSetup getSetup() {return setup;}

		//return all players
		@Nonnull
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> newPiece = new HashSet<>();
			newPiece.add(mrX.piece());
			for (Player playerDetective : detectives) {
				newPiece.add(playerDetective.piece());
			}
			return ImmutableSet.copyOf(newPiece);
		}

		@Nonnull
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player playerDetective : detectives) {
				if (playerDetective.piece().equals(detective)) return Optional.of(playerDetective.location());
			}
			return Optional.empty();
		}

		@Nonnull
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (piece.isMrX()) return Optional.of(ticket -> mrX.tickets().get(ticket));
			else if (piece.isDetective()) {
				for (Player playerDetective : detectives) {
					if (playerDetective.piece().equals(piece))
						return Optional.of(ticket -> playerDetective.tickets().get(ticket));
				}
			}
			return Optional.empty();
		}

		@Nonnull
		public ImmutableList<LogEntry> getMrXTravelLog(){return log;}

		@Nonnull
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
			//availableMoves for checking the number of left available moves
			Integer availableMoves = setup.moves.size() - log.size();

			if(player.has(ScotlandYard.Ticket.DOUBLE) && availableMoves >= 2) {
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
			}
			else {
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



		public static List<Integer> updateLocation(Move move){
			List<Integer> newDestination = new ArrayList<>();
			return move.accept(new Move.Visitor<List<Integer>>() {
				@Override
				public List<Integer> visit(Move.SingleMove move) {
					newDestination.add(move.destination);
					return newDestination;
				}

				@Override
				public List<Integer> visit(Move.DoubleMove move) {
					newDestination.add(move.destination1);
					newDestination.add(move.destination2);
					return newDestination;
				}
			});
		}

		public static List<ScotlandYard.Ticket> updateTicket(Move move){
			List< ScotlandYard.Ticket> newTicket = new ArrayList<>();
			return move.accept(new Move.Visitor<List<ScotlandYard.Ticket>>() {
				@Override
				public List<ScotlandYard.Ticket> visit(Move.SingleMove move) {
					newTicket.add(move.ticket);
					return newTicket;
				}

				@Override
				public List<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
					newTicket.add(move.ticket1);
					newTicket.add(move.ticket2);
					return newTicket;
				}
			});
		}

		@Nonnull
		public GameState advance(Move move) {
			//this.moves = getAvailableMoves();
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			//for updating log, ticket, location
			Set<Piece> updateRemaining = new HashSet<>();
			List<LogEntry> listLogEntry = new ArrayList<>();
			List<ScotlandYard.Ticket> addTicket = updateTicket(move);
			List<Integer> addLocation = updateLocation(move);

			//1. move should be added to the log
			//MrX's move
			if (move.commencedBy() == mrX.piece()) {
				//Doublemove
				if(addTicket.size() == 2){
					// checking the log only for reveal tickets
					//1. reveal + hidden
					if (setup.moves.get(log.size())) {
						listLogEntry.add(LogEntry.reveal(addTicket.get(0), addLocation.get(0)));
						listLogEntry.add(LogEntry.hidden(addTicket.get(1)));
					}
					//2. hidden + reveal
					else if (setup.moves.get(log.size() + 1)) {
						listLogEntry.add(LogEntry.hidden(addTicket.get(0)));
						listLogEntry.add(LogEntry.reveal(addTicket.get(1), addLocation.get(1)));
					}
					//3. hidden + hidden
					else {
						listLogEntry.add(LogEntry.hidden(addTicket.get(0)));
						listLogEntry.add(LogEntry.hidden(addTicket.get(1)));
					}
					//updating mrX location
					mrX.at(addLocation.get(1));
				}

				//Singlemove
				else {
					listLogEntry.add(LogEntry.reveal(addTicket.get(0), addLocation.get(0)));
					listLogEntry.add(LogEntry.hidden(addTicket.get(0)));
					//updating mrX location
					mrX.at(addLocation.get(0));
				}
				//adding up all the tickets that were used
				mrX.use(move.tickets());
				//move Mr X's position to their new destination
				return new MyGameState(setup, remaining, log, mrX, detectives);
			}

			//Detectives' move
			else {
				for(Player playerDetective : detectives){
					if(move.commencedBy() == playerDetective.piece()){
						listLogEntry.add(LogEntry.reveal(addTicket.get(0), addLocation.get(0)));
						playerDetective.use(move.tickets()).give(move.tickets());
					}
				}
			}

			log = ImmutableList.copyOf(listLogEntry);
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
	}



	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}


}
