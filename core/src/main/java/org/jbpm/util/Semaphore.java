package org.jbpm.util;

import java.io.Serializable;

/**
 * A counting semaphore. Conceptually, a semaphore maintains a set of permits. Each
 * {@linkplain #acquire acquire} blocks if necessary until a permit is available, and then takes it.
 * Each {@linkplain #release() release} adds a permit, potentially releasing a blocking acquirer.
 * 
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/concurrent/Semaphore.html"
 * >java.util.concurrent.Semaphore</a>
 */
public class Semaphore implements Serializable {

  private int permits;

  private static final long serialVersionUID = 1L;

  /**
   * Creates a <tt>Semaphore</tt> with the given number of permits.
   * 
   * @param permits the initial number of permits available. This value may be negative, in which
   *        case releases must occur before any acquires will be granted.
   */
  public Semaphore(int permits) {
    this.permits = permits;
  }

  /**
   * Releases a permit, returning it to the semaphore.
   */
  public void release() {
    release(1);
  }

  /**
   * Releases the given number of permits, returning them to the semaphore.
   * 
   * @param permits the number of permits to release
   * @throws IllegalArgumentException if permits less than zero.
   */
  public void release(int permits) {
    if (permits < 0) throw new IllegalArgumentException(Integer.toString(permits));

    synchronized (this) {
      this.permits += permits;
      notifyAll();
    }
  }

  /**
   * Acquires a permit from this semaphore, blocking until one is available, or the thread is
   * {@link Thread#interrupt interrupted}.
   * 
   * @throws InterruptedException if the current thread is interrupted
   * @see Thread#interrupt
   */
  public void acquire() throws InterruptedException {
    acquire(1);
  }

  /**
   * Acquires the given number of permits from this semaphore, blocking until all are available, or
   * the thread is {@link Thread#interrupt interrupted}.
   * 
   * @param permits the number of permits to acquire
   * @throws InterruptedException if the current thread is interrupted
   * @throws IllegalArgumentException if permits less than zero.
   * @see Thread#interrupt
   */
  public void acquire(int permits) throws InterruptedException {
    if (permits < 0) throw new IllegalArgumentException(Integer.toString(permits));

    synchronized (this) {
      while (this.permits < permits) {
        wait();
      }
      this.permits -= permits;
    }
  }

  /**
   * Acquires a permit from this semaphore, if one becomes available within the given waiting time
   * and the current thread has not been {@link Thread#interrupt interrupted}.
   * 
   * @param timeout the maximum time to wait for a permit
   * @return <tt>true</tt> if a permit was acquired and <tt>false</tt> if the waiting time elapsed
   *         before a permit was acquired.
   * @throws InterruptedException if the current thread is interrupted
   * @see Thread#interrupt
   */
  public boolean tryAcquire(long timeout) throws InterruptedException {
    return tryAcquire(1, timeout);
  }

  /**
   * Acquires the given number of permits from this semaphore, if all become available within the
   * given waiting time and the current thread has not been {@link Thread#interrupt interrupted}.
   * 
   * @param permits the number of permits to acquire
   * @param timeout the maximum time to wait for the permits
   * @return <tt>true</tt> if all permits were acquired and <tt>false</tt> if the waiting time
   *         elapsed before all permits were acquired.
   * @throws InterruptedException if the current thread is interrupted
   * @throws IllegalArgumentException if permits less than zero.
   * @see Thread#interrupt
   */
  public boolean tryAcquire(int permits, long timeout) throws InterruptedException {
    if (permits < 0) throw new IllegalArgumentException(Integer.toString(permits));

    long startTime = System.currentTimeMillis();
    synchronized (this) {
      while (this.permits < permits) {
        if (System.currentTimeMillis() - startTime >= timeout) return false;
        wait(timeout);
      }
      this.permits -= permits;
      return true;
    }
  }

  /**
   * Acquire and return all permits that are immediately available.
   * 
   * @return the number of permits
   */
  public synchronized int drainPermits() {
    int permits = this.permits;
    this.permits = 0;
    return permits;
  }
}
