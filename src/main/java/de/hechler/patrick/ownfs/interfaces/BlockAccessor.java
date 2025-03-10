package de.hechler.patrick.ownfs.interfaces;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

public interface BlockAccessor extends Closeable {
	
	/**
	 * returns the length of the arrays returned by {@link #loadBlock(long)}
	 * 
	 * @return the length of the arrays returned by {@link #loadBlock(long)}
	 */
	int blockSize();
	
	/**
	 * returns the value of the block at the position {@code block}<br>
	 * all changes on the returned array possibly have a direct impact on the {@link BlockAccessor}, but the {@link #saveBlock(byte[], long)} or {@link #unloadBlock(long)} method has to be called
	 * after calling this method with the returned block and the position.<br>
	 * 
	 * the behavior is undefined if this method is called twice without calling {@link #saveBlock(byte[], long)} or {@link #unloadBlock(long)} between the calls.
	 * 
	 * @param block
	 *            the position of the block returned
	 * @return the value of the block
	 * @throws IOException
	 *             if an IO error occurs during the operation
	 * @throws ClosedChannelException
	 *             if {@link #close()} was already called
	 */
	byte[] loadBlock(long block) throws IOException, ClosedChannelException;
	
	/**
	 * saves the block after changing it.<br>
	 * 
	 * the behavior is undefined if the {@code value} array is not the same array as the array previously returned by {@link #loadBlock(long)} with the same {@code block} or if the block has already
	 * been unloaded/saved.
	 * 
	 * @param value
	 *            the value of the block
	 * @param block
	 *            the position of the block
	 * @throws IOException
	 *             if an IO error occurs during the operation
	 * @throws ClosedChannelException
	 *             if {@link #close()} has been called already
	 */
	void saveBlock(byte[] value, long block) throws IOException, ClosedChannelException;
	
	/**
	 * unloads the given block without saving it.<br>
	 * 
	 * the behavior is undefined if the {@code block} has not been {@link #loadBlock(long) loaded} before or the block has been unloaded already using the {@link #saveBlock(byte[], long)} or
	 * {@link #unloadBlock(long)} method.
	 * 
	 * @param block
	 *            the block to unload
	 * @throws ClosedChannelException
	 *             if {@link #close()} has been called already
	 */
	void unloadBlock(long block) throws ClosedChannelException;
	
	/**
	 * unloads all loaded blocks without saving them. If they should be saved use {@link #saveAll()}.
	 * 
	 * further calls to {@link #loadBlock(long)}, {@link #saveBlock(byte[], long)}, {@link #unloadBlock(long)} and {@link #saveAll()} will throw an {@link ClosedChannelException}.
	 * <p>
	 * 
	 * Closes this stream and releases any system resources associated with it. If the stream is already closed then invoking this method has no effect.
	 * <p>
	 * 
	 * As noted in {@link AutoCloseable#close()}, cases where the close may fail require careful attention. It is strongly advised to relinquish the underlying resources and to internally mark the
	 * {@link Closeable} as closed, prior to throwing the IOException.
	 * 
	 */
	@Override
	void close();
	
	/**
	 * saves and unloads all blocks, but does not closes this {@link BlockAccessor}
	 * 
	 * @throws IOException
	 * @throws ClosedChannelException
	 *             if this is closed
	 */
	void saveAll() throws IOException, ClosedChannelException;
	
	/**
	 * unloads all loaded blocks without saving them.
	 * 
	 * @throws ClosedChannelException
	 *             if this is closed
	 */
	void unloadAll() throws ClosedChannelException;
	
}
