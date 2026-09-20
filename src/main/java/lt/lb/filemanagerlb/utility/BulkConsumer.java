package lt.lb.filemanagerlb.utility;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author laim0nas100
 */
public interface BulkConsumer<T> extends Consumer<T> {

    public default void acceptAll(List<T> list) {
        for (T element : list) {
            accept(element);
        }
    }

    public default BulkConsumer<T> sync() {
        final BulkConsumer<T> me = this;
        return new BulkConsumer<T>() {
            @Override
            public synchronized void accept(T t) {
                me.accept(t);
            }

            @Override
            public synchronized void acceptAll(List<T> list) {
                me.acceptAll(list);
            }
        };
    }

    public static <A> BulkConsumer<A> sync(Consumer<A> cons) {
        return new BulkConsumer<A>() {
            @Override
            public synchronized void accept(A t) {
                cons.accept(t);
            }

            @Override
            public synchronized void acceptAll(List<A> list) {
                BulkConsumer.super.acceptAll(list);
            }
        };
    }

    public static <A> BulkConsumer<A> fromCollection(Collection<A> collection) {
        return new BulkConsumer<A>() {
            @Override
            public void accept(A t) {
                collection.add(t);
            }

            @Override
            public void acceptAll(List<A> list) {
                collection.addAll(list);
            }
        };
    }
}
