package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	private final class MyModel implements Model{
		private Observer.Event event;
		private ImmutableSet<Observer> observers;
		private GameSetup setup;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private Board.GameState gameState;

		//constructor that builds MyModel
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
			//throwing an exception
			if(observers.contains(observer)) throw new IllegalArgumentException("This observer is already registered!");
			updatedObservers.add(observer);
			this.observers = ImmutableSet.copyOf(updatedObservers);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			Set<Observer> updatedObservers = new HashSet<>(observers);
			//throwing exceptions
			if(observers.isEmpty()) throw new NullPointerException("Empty observers!");
			if(!observers.contains(observer)) throw new IllegalArgumentException("This observer was never registered!");
			updatedObservers.remove(observer);
			this.observers = ImmutableSet.copyOf(updatedObservers);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return this.observers;
		}


		@Override
		public void chooseMove(@Nonnull Move move) {
			// TODO Advance the model with move, then notify all observers of what what just happened.
			//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
			this.gameState = gameState.advance(move);
			if(gameState.getWinner().isEmpty()){
				event = Observer.Event.MOVE_MADE;
			}
			else {
				event = Observer.Event.GAME_OVER;
			}
			for(Observer observer : observers){
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
