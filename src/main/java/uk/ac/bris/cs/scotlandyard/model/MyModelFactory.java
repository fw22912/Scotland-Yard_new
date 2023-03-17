package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	private final class MyModel implements Model{
		private Board.GameState gameState;
		private ImmutableSet<Observer> observers;

		private MyModel(
				final GameSetup setup,
				final Player mrX,
				final ImmutableList<Player> detectives){
			this.gameState =  new MyGameStateFactory().build(setup, mrX, detectives);
			this.observers = ImmutableSet.of();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return gameState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			Set<Observer> updatedObservers = new HashSet<>(observers);
			if (observer == null) throw new NullPointerException("testUnregisterNullObserverShouldThrow: Observer is empty!");
			if (observers.contains(observer)) throw new IllegalArgumentException("testRegisterSameObserverTwiceShouldThrow: This observer is already registered!");
			updatedObservers.add(observer);
			observers = ImmutableSet.copyOf(updatedObservers);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			Set<Observer> updatedObservers = new HashSet<>(observers);
			updatedObservers.remove(observer);
			if (observer == null) throw new NullPointerException("testUnregisterNullObserverShouldThrow: Observer is empty!");
			if (!observers.contains(observer)) throw new IllegalArgumentException("testUnregisterIllegalObserverShouldThrow: This observer is already removed!");
			observers = ImmutableSet.copyOf(updatedObservers);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}


		@Override
		public void chooseMove(@Nonnull Move move) {
			// TODO Advance the model with move, then notify all observers of what what just happened.
			//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
			Observer.Event event;
			this.gameState = gameState.advance(move);
			if (!gameState.getWinner().isEmpty()) {
				event = Observer.Event.GAME_OVER;
			}
			else {
				event = Observer.Event.MOVE_MADE;}

			for(Observer observer : observers) {
				observer.onModelChanged(gameState, event);
			}
		}
	}
	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}


}
