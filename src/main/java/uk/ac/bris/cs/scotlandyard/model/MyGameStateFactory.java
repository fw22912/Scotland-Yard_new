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
import java.util.stream.Stream;

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
			this.winner = ImmutableSet.of();

			//throwing exceptions
			if (mrX == null) throw new NullPointerException("testNullMrXShouldThrow: mrX is empty!");
			if (detectives == null)
				throw new NullPointerException("testNullDetectiveShouldThrow: Detectives are empty!");
			if (mrX.isDetective()) throw new IllegalArgumentException("testNoMrXShouldThrow: MrX is a detective!");
			Set<Player> set = new HashSet<>();
			Set<Integer> setLocation = new HashSet<>();
			detectives.forEach(playerDetective -> {
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
			});
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("testEmptyMovesShouldThrow: Moves is empty!");
			if (setup.graph.nodes().isEmpty())
				throw new IllegalArgumentException("testEmptyGraphShouldThrow:Empty graph!");
		}

		//implementing methods
		@Nonnull
		public GameSetup getSetup() {return setup;}

		//return all players
		@Nonnull
		public ImmutableSet<Piece> getPlayers() {
//			Set<Piece> newPiece = new HashSet<>();
//			newPiece.add(mrX.piece());
//			for (Player playerDetective : detectives) {
//				newPiece.add(playerDetective.piece());
//			}
//			return ImmutableSet.copyOf(newPiece);
			return Stream.concat(
					Stream.of(mrX.piece()),
					detectives.stream().map(Player :: piece)
			).collect(ImmutableSet.toImmutableSet());
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
			Integer availableMoves = setup.moves.size() - log.size();
			Set<Piece> detectivePiece = new HashSet<>();
			detectives.forEach(playerDetective -> detectivePiece.add(playerDetective.piece()));
			Set<Piece> finalWinner = new HashSet<>();

			//MrX wins
			//no more available ticket
////			for (int i = 0; i <= detectives.size(); i++) {
//				for (Player playerDetective : detectives)
//					if (playerDetective.tickets().isEmpty()) {
//						finalWinner.add(mrX.piece());
//						winner = ImmutableSet.copyOf(finalWinner);
//					}
////			}

			//no move players
			if(getAvailableMoves().isEmpty() && remaining.contains(mrX.piece())) {
				finalWinner.add(mrX.piece());
				winner = ImmutableSet.copyOf(finalWinner);
			}

			//no rounds
			if(setup.moves.size() - log.size() == 0){
				finalWinner.add(mrX.piece());
				winner = ImmutableSet.copyOf(finalWinner);}

			//Detectives win
			//1. detective finish a move on the same station as Mr X
			for (Player playerDetective : detectives) {
				//mrX being caught
				if (playerDetective.location() == this.mrX.location()) {
					winner = ImmutableSet.copyOf(detectivePiece);
				}
				//no more available moves for MrX
				if(getAvailableMoves().isEmpty() && !remaining.contains(mrX.piece())) {
					winner = ImmutableSet.copyOf(detectivePiece);
				}
			}
			return winner;
		}


		//helper method for Singlemove
		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			Set<Move.SingleMove> availableMoves = new HashSet<>();
			Set<Integer> detectiveOccupied = new HashSet<>();

			detectives.forEach(detective -> detectiveOccupied.add(detective.location()));

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
			return ImmutableSet.copyOf(availableMoves);
		}

		//helper method for Doublemove
		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log) {
			Set<Move.DoubleMove> doubleMoves = new HashSet<>();
			//storing firstMoves
			Set<Move.SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);
			Set<Move.SingleMove> secondMoves = new HashSet<>();
			//availableMoves for checking the number of left available moves
			Integer availableMoves = setup.moves.size() - log.size();

			if(player.has(ScotlandYard.Ticket.DOUBLE) && availableMoves >= 2) {
				for (Move.SingleMove firstMove : firstMoves) {
					secondMoves = makeSingleMoves(setup, detectives, player, firstMove.destination);
					for (Move.SingleMove secondMove : secondMoves) {
						if ((player.hasAtLeast(firstMove.ticket, 2)
								&& firstMove.ticket == secondMove.ticket)
								|| (firstMove.ticket != secondMove.ticket
								&& player.hasAtLeast(firstMove.ticket, 1)
								&& player.hasAtLeast(secondMove.ticket, 1))) {
							Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, firstMove.ticket, firstMove.destination, secondMove.ticket, secondMove.destination);
							doubleMoves.add(doubleMove);
						}
					}
				}
			}
			if(firstMoves.isEmpty()){
				return ImmutableSet.of();
			}

			return ImmutableSet.copyOf(doubleMoves);
		}

		/*getAvailableMoves*/
		@Nonnull
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> newMoves = new HashSet<>();

			if(!winner.isEmpty()) return ImmutableSet.of();

			System.out.println("Checking this.remaining...");
			System.out.println(this.remaining);
			if (this.remaining.contains(mrX.piece())) {
				System.out.println("Reached 2a");
				//How to make single moves
				Set<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectives, mrX, mrX.location());
				Set<Move.DoubleMove> doubleMoves = makeDoubleMoves(setup, detectives, mrX, mrX.location(), log);
				newMoves.addAll(singleMoves);
				newMoves.addAll(doubleMoves);
			}
			else {
				System.out.println("Reached 2b");
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


		//helper method for updating the location
		public static List<Integer> updateLocation(Move move){
			return move.accept(new Move.Visitor<List<Integer>>() {
				List<Integer> newDestination = new ArrayList<>();
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

		//helper method for updating ticket
		public static List<ScotlandYard.Ticket> updateTicket(Move move){
			return move.accept(new Move.Visitor<List<ScotlandYard.Ticket>>() {
				List< ScotlandYard.Ticket> newTicket = new ArrayList<>();

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

		//advance
		@Nonnull
		public GameState advance(Move move) {
			System.out.println("Before...");
			System.out.println(move);
			this.moves = this.getAvailableMoves();

			//for updating log, ticket, location, remaining, location, new detectives after move
			List<LogEntry> listLogEntry = new ArrayList<>(log);
			List<ScotlandYard.Ticket> addTicket = updateTicket(move);
			List<Integer> addLocation = updateLocation(move);
			Set<Piece> updatedRemaining = new HashSet<>();
			List<Player> updateDetectives = new ArrayList<>();

			System.out.println("Testing...");
			System.out.println(this.moves);
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			//1. move should be added to the log
			//MrX's move
			if (move.commencedBy() == mrX.piece() && remaining.contains(mrX.piece())) {
				//Singlemove
				if(addLocation.size() == 1){
					if(setup.moves.get(log.size())){
						listLogEntry.add(LogEntry.reveal(addTicket.get(0), addLocation.get(0)));
					}
					else listLogEntry.add(LogEntry.hidden(addTicket.get(0)));
					//updating mrX location
					//move Mr X's position to their new destination
					mrX = mrX.at(addLocation.get(0));
				}

				//Doublemove
				else{
					// checking the log only for reveal tickets
					//check if mrX is using double ticket
					//reveal: True hidden: False
					//1. reveal + reveal << Why do we need this
					if(setup.moves.get(log.size()) && setup.moves.get(log.size()+1)){
						listLogEntry.add(LogEntry.reveal(addTicket.get(0), addLocation.get(0)));
						listLogEntry.add(LogEntry.reveal(addTicket.get(1), addLocation.get(1)));
					}
					//2. reveal + hidden
					else if (setup.moves.get(log.size())) {
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
					mrX = mrX.at(addLocation.get(1));
				}
				//adding up all the tickets that were used
				mrX = mrX.use(move.tickets());
				//updating remaining by eliminating mrX.piece
				detectives.forEach(playerDetective -> updatedRemaining.add(playerDetective.piece()));
				remaining = ImmutableSet.copyOf(updatedRemaining);
				System.out.println("Hi!");
				System.out.println(detectives);
				log = ImmutableList.copyOf(listLogEntry);

				return new MyGameState(setup, remaining, log, mrX, detectives);
			}

			//Detectives' move
			else {
				for(Player playerDetective : detectives) {
					 if (remaining.contains(playerDetective.piece())) {
						 if (remaining.contains(move.commencedBy())) {
							 //if playerDetective can move
							 if (move.commencedBy() == playerDetective.piece()) {
								 //give the used ticket to mrX
								 playerDetective = playerDetective.use(addTicket.get(0));
								 mrX = mrX.give((addTicket.get(0)));
								 //update moved detectives for returning state(if), and remaining pieces
								 playerDetective = playerDetective.at(addLocation.get(0));
							 }
							 //if playerDetective cannot move do not update location, tickets but only remaining and detectives
							 else {
								 updatedRemaining.add(playerDetective.piece());
							 }
						 }
					 }

					updateDetectives.add(playerDetective);
				}

				if (updatedRemaining.isEmpty()) {
					remaining = ImmutableSet.of(mrX.piece());
				} else {
					remaining = ImmutableSet.copyOf(updatedRemaining);
				}

				detectives = ImmutableList.copyOf(updateDetectives);
				return new MyGameState(setup, remaining, log, mrX, detectives);

			}
		}
	}



	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}


}
