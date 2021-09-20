package rim.core;

import java.util.Iterator;

/**
 * Flagのベンチマーク確認関連.
 */
public class FlagsBench {
	// main.
	public static final void main(String[] args) {
		bench(args);
		//check(args);
	}
	
	// チェック処理.
	protected static final void check(String[] args) {
		boolean ascFlag = false;
		int allLen = 5000000;
		
		final int[] posList = new int[] {
			0, 3332, 10000, 50000, 88000, 325442, 988824, 999999
		};
		//final BaseFlags<?> flags = new Flags(allLen);
		//final BaseFlags<?> flags = new LargeFlags(allLen);
		final BaseFlags<?> flags = new SmartFlags(allLen);
		
		final int len = posList.length;
		for(int i = 0; i < len; i ++) {
			flags.put(posList[i], true);
		} 
		
		for(int i = 0; i < len; i ++) {
			System.out.println("[" + posList[i] + "]: " + flags.get(posList[i]));
		}
		
		final Iterator<Integer> itr = flags.iterator(ascFlag);
		while(itr.hasNext()) {
			System.out.println(itr.next());
		}
	}
	
	// 計測系ベンチマーク.
	protected static final void bench(String[] args) {
		final int roopAll = 5;
		final int roop = 1000;
		
		long t, a, b, c, d, e, f;
		
		int allLen = 1000000;
		
		//final int[] posList = new int[] {
		//	0, 3332, 10000, 50000, 88000, 325442, 988824, 999999
		//};
		int cnt = 0;
		int waru = 100;
		Integer[] posList = new Integer[(allLen / waru) + ((allLen % waru) != 0 ? 1 : 0)];
		for(int i = 0; i < allLen; i += waru) {
			posList[cnt ++] = i;
		}
		
		BaseFlags<?> flags = null;
		for(int btype = 0; btype < 3; btype ++) {
		a = b = c = d = e = f = 0L;
		for(int xx = 0; xx < roopAll; xx ++) {
		for(int r = 0; r < roop; r ++) {
			
			t = System.currentTimeMillis();
			switch(btype) {
			case 0:
				flags = new Flags(allLen);
				break;
			case 1:
				flags = new LargeFlags(allLen);
				break;
			case 2:
				flags = new SmartFlags(allLen);
				break;
			}
			a += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			final int len = posList.length;
			for(int i = 0; i < len; i ++) {
				flags.put(posList[i], true);
			}
			b += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			final Iterator<Integer> itr = flags.iterator(false);
			while(itr.hasNext()) {
				itr.next();
			}
			c += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			for(int i = 0; i < len; i ++) {
				if(flags.get(posList[i])) {}
			}
			d += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			final boolean[] testFlag = new boolean[allLen];
			for(int i = 0; i < len; i ++) {
				testFlag[posList[i]] = true;
			}
			e += System.currentTimeMillis() - t;
			
			t = System.currentTimeMillis();
			for(int i = 0; i < len; i ++) {
				if(testFlag[posList[i]]) {};
			}
			f += System.currentTimeMillis() - t;
		}}
		
		System.out.println(flags.getClass().getName() + "[" + roopAll + " / " + roop + "]");
		System.out.println();
		System.out.println("create: " + a + " msec");
		System.out.println("put: " + b + " msec");
		System.out.println("iterator.next: " + c + " msec");
		System.out.println("get: " + d + " msec");
		System.out.println("create.booleanArray: " + e + " msec");
		System.out.println("get.booleanArray: " + f + " msec");
		System.out.println();
		}
	}
}
