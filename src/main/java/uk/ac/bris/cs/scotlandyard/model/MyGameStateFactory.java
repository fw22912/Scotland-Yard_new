package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;
import java.util.stream.Collectors;
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
		private List<LogEntry> listLogEntry;
		private Set<Piece> updatedRemaining;
		private List<Player> updateDetectives;


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
			this.listLogEntry = new ArrayList<>(log);
			this.updatedRemaining = new HashSet<>();
			this.updateDetectives = new ArrayList<>();


			// Throwing exceptions
			Set<Player> set = new HashSet<>();
			Set<Integer> setLocation = new HashSet<>();
			if (mrX == null || detectives == null) throw new NullPointerException();
			if (mrX.isDetective()) throw new IllegalArgumentException();
			detectives.forEach(playerDetective -> {
				if(playerDetective == null) throw new NullPointerException();
				if (playerDetective.isMrX())
					throw new IllegalArgumentException();
				if (playerDetective.has(ScotlandYard.Ticket.DOUBLE) || playerDetective.has(ScotlandYard.Ticket.SECRET))
					throw new IllegalArgumentException();
				if (!set.add(playerDetective) || !setLocation.add(playerDetective.location()))
					throw new IllegalArgumentException();
			});
			if (setup.moves.isEmpty() || setup.graph.nodes().isEmpty()) throw new IllegalArgumentException();

			// Determine winner
			Set<Piece> detectivePiece = new HashSet<>();
			Set<Move> allMoves = new HashSet<>();
			Set<Piece> mrXPiece = Set.of(mrX.piece());
			List<Player> onlyMrX = List.of(mrX);
			detectives.forEach(playerDetective -> detectivePiece.add(playerDetective.piece()));

			//Detectives win
			//1. detectives catch mrX
			if(detectives.stream().anyMatch(playerDetective-> playerDetective.location() == mrX.location())){
				winner = ImmutableSet.copyOf(detectivePiece);
			}
			//2. no unoccupied stations for MrX to travel to
			if(remaining.contains(mrX.piece()) && getAvailableMoves(onlyMrX).isEmpty()){
				winner = ImmutableSet.copyOf(detectivePiece);
			}
			//MrX wins
			//1. Detectives cannot move
			else if(getAvailableMoves(detectives).isEmpty()){
				winner = ImmutableSet.copyOf(mrXPiece);
			}
			//mrX managed to fill the log and detectives failed to catch mrX
			else if(remaining.contains(mrX.piece()) && setup.moves.size() == log.size()){
				winner = ImmutableSet.copyOf(mrXPiece);
			}
			else {
				Set<Piece> originalRemaining = new HashSet<>(this.remaining);
				//iterate through the pieces in the remaining and add their possible moves if not null
				for(Piece piece : originalRemaining){
					Player currentPlayer = pieceMatchesPlayer(piece);
					assert currentPlayer != null;
					if(!getAvailableMoves(List.of(currentPlayer)).isEmpty()){
						allMoves.addAll(getAvailableMoves(List.of(currentPlayer)));
					}
				}
				//detectives cannot move(
				if(allMoves.isEmpty()){
					//all players cannot move
					if(getAvailableMoves(detectives).isEmpty()){
						winner = ImmutableSet.copyOf(mrXPiece);
					}
					//some detectives that is not in the remaining can move
					//game does not stop
					else{
						//adding moves of every detective
						allMoves.addAll(detectives.stream()
								.map(playerDetective -> getAvailableMoves(List.of(playerDetective)))
								.flatMap(Collection :: stream)
								.collect(Collectors.toSet()));
						//if mrX is cornered
						if(getAvailableMoves(onlyMrX).isEmpty()){
							winner = ImmutableSet.copyOf(detectivePiece);
						}
						// detectives cannot move
						else{
							this.remaining = ImmutableSet.copyOf(mrXPiece);
						}
					}
				}
			}
		}

		//implementing methods
		@Nonnull
		public GameSetup getSetup() {return setup;}

		//return all players
		@Nonnull
		public ImmutableSet<Piece> getPlayers() {
			return Stream.concat(Stream.of(mrX.piece()), detectives.stream().map(Player :: piece))
					.collect(ImmutableSet.toImmutableSet());
		}

		@Nonnull
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			return detectives.stream()
					.filter(player -> player.piece().equals(detective))
					.map(Player::location)
					.findFirst();
		}

		@Nonnull
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (piece.isMrX()) {
				return Optional.of(ticket -> mrX.tickets().get(ticket));
			} else {
				return detectives.stream()
						.filter(player -> player.piece().equals(piece))
						.findFirst()
						.map(player -> ticket -> player.tickets().get(ticket));
			}
		}

		@Nonnull
		public ImmutableList<LogEntry> getMrXTravelLog() {return log;}

		@Nonnull
		public ImmutableSet<Piece> getWinner() {return winner;}


		@Nonnull
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> newMoves = new HashSet<>();

			if(!winner.isEmpty()) return ImmutableSet.of();

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

		//HELPER FUNCTION//
		//returns available moves of certain players
		private <T extends Player> ImmutableSet<Move> getAvailableMoves(List<T> movesWanted){
			Set<Move> thisMove = new HashSet<>();
			for(T player : movesWanted){
				Set<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectives, player, player.location());
				if(this.remaining.contains(mrX.piece())){
					Set<Move.DoubleMove> doubleMoves = makeDoubleMoves(setup, detectives, player, player.location(), log);
					thisMove.addAll(singleMoves);
					thisMove.addAll(doubleMoves);
				}
				else{
					thisMove.addAll(singleMoves);
				}
			}
			return ImmutableSet.copyOf(thisMove);
		}

		private Player pieceMatchesPlayer(Piece piece) {
			Set<Player> allPlayers = Stream.concat(detectives.stream(), Stream.of(mrX))
					.collect(Collectors.toSet());
			return allPlayers.stream()
					.filter(player -> player.piece().equals(piece))
					.findFirst()
					.orElse(null);
		}


		//SingleMove
		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<Move.SingleMove> availableMoves = new HashSet<>();
			Set<Integer> detectiveOccupied = new HashSet<>();

			detectives.forEach(detective -> detectiveOccupied.add(detective.location()));

			//checks whether the location is occupied, and has available tickets and add the move
			for (int destination : setup.graph.adjacentNodes(source)) {
				if (!detectiveOccupied.contains(destination)) {
					for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						if (player.has(t.requiredTicket())) {
							Move.SingleMove newMoves = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
							availableMoves.add(newMoves);
						}
					}
					//check if the player has secret ticket, and if yes use it
					if (player.has(ScotlandYard.Ticket.SECRET) && player.isMrX()) {
						Move.SingleMove newMoves = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
						availableMoves.add(newMoves);
					}
				}
			}
			return ImmutableSet.copyOf(availableMoves);
		}

		//DoubleMove
		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log) {
			Set<Move.DoubleMove> doubleMoves = new HashSet<>();
			//storing firstMoves
			Set<Move.SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);
			Set<Move.SingleMove> secondMoves;
			//availableMoves for checking the number of left available moves
			int availableMoves = setup.moves.size() - log.size();

			if(player.has(ScotlandYard.Ticket.DOUBLE) && availableMoves >= 2) {
				for (Move.SingleMove firstMove : firstMoves) {
					//create second moves based on first move's destination
					secondMoves = makeSingleMoves(setup, detectives, player, firstMove.destination);
					for (Move.SingleMove secondMove : secondMoves) {
						//if it uses same transportation check at least two tickets, otherwise check one
						if ((player.hasAtLeast(firstMove.ticket, 2) && firstMove.ticket == secondMove.ticket)
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

		//updating the location
		private static List<Integer> updateLocation(Move move){
			return move.accept(new Move.Visitor<>() {
				final List<Integer> newDestination = new ArrayList<>();
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

		//updating ticket
		private static List<ScotlandYard.Ticket> updateTicket(Move move){
			return move.accept(new Move.Visitor<>() {
				final List< ScotlandYard.Ticket> newTicket = new ArrayList<>();

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

		//updates gamestate after mrX's move
		private GameState mrXMove(Move move) {
			//for updating log, ticket, location, remaining, location, new detectives after move
			List<ScotlandYard.Ticket> addTicket = updateTicket(move);
			List<Integer> addLocation = updateLocation(move);

			//1. move should be added to the log
			//SingleMove
			if(addLocation.size() == 1){
				if(setup.moves.get(log.size())){
					listLogEntry.add(LogEntry.reveal(addTicket.get(0), addLocation.get(0)));
				}
				else listLogEntry.add(LogEntry.hidden(addTicket.get(0)));
				//updating mrX location
				//move Mr X's position to their new destination
				mrX = mrX.at(addLocation.get(0));
			}

			//DoubleMove
			else{
				//check if mrX is using double ticket - reveal: True hidden: False
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
			log = ImmutableList.copyOf(listLogEntry);

			return new MyGameState(setup, remaining, log, mrX, detectives);
		}

		//updates gamestate after detective's move
		private GameState detectivesMove(Move move) {
			//for updating log, ticket, location, remaining, location, new detectives after move
			List<ScotlandYard.Ticket> addTicket = updateTicket(move);
			List<Integer> addLocation = updateLocation(move);

			//Detectives' move
			for(Player playerDetective : detectives) {
				if (remaining.contains(playerDetective.piece()) && remaining.contains(move.commencedBy())) {
					//if playerDetective can move
					if (move.commencedBy() == playerDetective.piece()) {
						//give the used ticket to mrX
						playerDetective = playerDetective.use(addTicket.get(0));
						mrX = mrX.give((addTicket.get(0)));
						//update moved detectives for returning state(if), and remaining pieces
						playerDetective = playerDetective.at(addLocation.get(0));
					}
					//if playerDetective cannot move do not update location, tickets but only remaining and detectives
					else updatedRemaining.add(playerDetective.piece());
				}
				updateDetectives.add(playerDetective);
			}
			//check whether updated remaining is empty or not
			if (updatedRemaining.isEmpty()) {remaining = ImmutableSet.of(mrX.piece());}
			else {remaining = ImmutableSet.copyOf(updatedRemaining);}

			detectives = ImmutableList.copyOf(updateDetectives);

			return new MyGameState(setup, remaining, log, mrX, detectives);
		}

		//HELPER METHOD END//
		@Nonnull
		public GameState advance(Move move) {
			this.moves = this.getAvailableMoves();
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			if (move.commencedBy() == mrX.piece() && remaining.contains(mrX.piece())) {
				return mrXMove(move);
			}
			else return detectivesMove(move);
		}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}