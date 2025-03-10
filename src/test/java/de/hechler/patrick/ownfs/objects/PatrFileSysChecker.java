package de.hechler.patrick.ownfs.objects;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;
import de.hechler.patrick.ownfs.objects.PatrFileSys.FolderElement;
import de.hechler.patrick.ownfs.objects.PatrFileSys.PatrFile;
import de.hechler.patrick.ownfs.objects.PatrFileSys.PatrFolder;
import de.hechler.patrick.zeugs.check.Checker;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;


@CheckClass
public class PatrFileSysChecker extends Checker {
	
	BlockAccessor ba;
	PatrFileSys   pfs;
	
	@Start
	private void start() {
		pfs = null;
		ba = null;
	}
	
	@End
	private void end() {
		pfs = null;
		ba = null;
	}
	
	@Check
	private void check() throws IOException {
		ba = new BlockAccessorByteArrayArrayImpl(16, 256);
		pfs = PatrFileSys.createNewFileSys(ba, 16);
		PatrFolder root = pfs.rootFolder();
		FolderElement element = root.addElement("myFirstFile.txt", true);
		assertEquals("myFirstFile.txt", element.getName());
		element.setName("newName.txt");
		assertEquals("newName.txt", element.getName());
		PatrFile file = element.getFile();
		byte[] bytes = "hello world".getBytes();
		file.append(bytes, 0, bytes.length);
		byte[] read = new byte[bytes.length];
		file.read(read, 0, 0L, read.length);
		assertArrayEquals(bytes, read);
		read = new byte[bytes.length + 10];
		file.read(read, 5, 0L, read.length - 10);
		assertArrayEquals(bytes, Arrays.copyOfRange(read, 5, read.length - 5));
		element = root.addElement("mySecondFile.txt", true);
		assertEquals("mySecondFile.txt", element.getName());
		file = element.getFile();
		bytes = ("hello!\n"
			+ "This is a big text which needs to be saved in multiple blocks!\n"
			+ "Even if the text is not that huge it needs to be saved in multiple blocks, because the blocks are very small.\n"
			+ "Every blocks can only save 256 bytes!\n"
			+ "and now again:\n"
			+ "hello!\n"
			+ "This is a big text which needs to be saved in multiple blocks!\n"
			+ "Even if the text is not that huge it needs to be saved in multiple blocks, because the blocks are very small.\n"
			+ "Every blocks can only save 256 bytes!\n"
			+ "and now again: (not)\n").getBytes();
		file.append(bytes, 0, bytes.length);
		read = new byte[bytes.length];
		file.read(read, 0, 0L, read.length);
		assertArrayEquals(bytes, read);
		element = root.addElement("myFolder", false);
		assertEquals("myFolder", element.getName());
		PatrFolder folder = element.getFolder();
		element = folder.addElement("subFile.txt", true);
		file = element.getFile();
		bytes = ("this is a sub file!\n"
			+ "the files path is:\n"
			+ "'myFolder' --> 'subFile.txt'\n"
			+ "this can also be written as:\n"
			+ "'myFolder\0subFile.txt'\n"
			+ "but normally this will be shown as:\n"
			+ "'/myFolder/subFile.txt'\n"
			+ "or as:\n"
			+ "'\\myFolder\\subFile.txt'\n"
			+ "these are all four/three possibilities to show this files path\n"
			+ "").getBytes();
		file.append(bytes, 0, bytes.length);
		read = new byte[bytes.length];
		file.read(read, 0, 0L, bytes.length);
		assertArrayEquals(bytes, read);
		byte[] over = ("[OVERWRITE]: this part of the file has been overwritten.\n[OVERWRITE_END]\n").getBytes();
		read = new byte[over.length];
		file.overwrite(over, 0, 10L, over.length);
		file.read(read, 0, 10L, over.length);
		assertArrayEquals(over, read);
		System.arraycopy(over, 0, bytes, 10, over.length);
		read = new byte[bytes.length];
		file.read(read, 0, 0L, bytes.length);
		assertArrayEquals(bytes, read);
		file.remove(50L);
		bytes = Arrays.copyOf(bytes, bytes.length - 50);
		read = new byte[bytes.length];
		file.read(read, 0, 0L, bytes.length);
		assertArrayEquals(bytes, read);
	}
	
	private static class BlockAccessorByteArrayArrayImpl implements BlockAccessor {
		
		private byte[][]              blocks;
		private Map <Integer, byte[]> loaded;
		
		
		public BlockAccessorByteArrayArrayImpl(int blockCnt, int blockSize) {
			this.blocks = new byte[blockCnt][blockSize];
			this.loaded = new HashMap <>();
		}
		
		@Override
		public int blockSize() {
			return this.blocks[0].length;
		}
		
		@Override
		public byte[] loadBlock(final long block) throws IOException, IndexOutOfBoundsException, ClosedChannelException {
			if (this.loaded == null) {
				throw new ClosedChannelException();
			}
			if (block >= this.blocks.length) {
				throw new IndexOutOfBoundsException("blockcount=" + this.blocks.length + " block=" + block);
			}
			final int intblock = (int) block;
			final byte[] clone = this.blocks[intblock].clone();
			final byte[] old = loaded.putIfAbsent(Integer.valueOf(intblock), clone);
			if (old != null) {
				throw new IOException("block already loaded");
			}
			return clone;
		}
		
		@Override
		public void saveBlock(byte[] value, long block) throws IOException, ClosedChannelException {
			if (this.loaded == null) {
				throw new ClosedChannelException();
			}
			if (block >= this.blocks.length) {
				throw new IndexOutOfBoundsException("blockcount=" + this.blocks.length + " block=" + block);
			}
			final int intblock = (int) block;
			final boolean removed = loaded.remove(Integer.valueOf(intblock), value);
			if ( !removed) {
				throw new IOException("block not loaded");
			}
			System.arraycopy(value, 0, this.blocks[intblock], 0, value.length);
		}
		
		@Override
		public void unloadBlock(long block) throws ClosedChannelException {
			if (this.loaded == null) {
				throw new ClosedChannelException();
			}
			if (block >= this.blocks.length) {
				throw new IndexOutOfBoundsException("blockcount=" + this.blocks.length + " block=" + block);
			}
			final int intblock = (int) block;
			final byte[] removed = loaded.remove(Integer.valueOf(intblock));
			if (removed == null) {
				throw new IllegalStateException("block not loaded");
			}
		}
		
		@Override
		public void close() {
			this.loaded = null;
		}
		
		@Override
		public void saveAll() throws IOException, ClosedChannelException {
			if (this.loaded == null) {
				throw new ClosedChannelException();
			}
			for (Entry <Integer, byte[]> e : this.loaded.entrySet()) {
				saveBlock(e.getValue(), e.getKey().intValue());
			}
		}
		
		@Override
		public void unloadAll() throws ClosedChannelException {
			if (this.loaded == null) {
				throw new ClosedChannelException();
			}
			this.loaded.clear();
		}
		
	}
	
}
