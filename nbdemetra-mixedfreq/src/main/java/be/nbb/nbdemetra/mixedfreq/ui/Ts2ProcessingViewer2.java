/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.nbb.nbdemetra.mixedfreq.ui;

import be.nbb.demetra.mixedfreq.document.MultiTsDocument2;
import ec.nbdemetra.ui.MonikerUI;
import ec.tss.Ts;
import ec.tss.TsMoniker;
import ec.tss.datatransfer.TssTransferSupport;
import ec.tstoolkit.algorithm.IProcSpecification;
import ec.ui.view.tsprocessing.DefaultProcessingViewer;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.TransferHandler;

/**
 *
 * @author Jean Palate
 */
public class Ts2ProcessingViewer2 extends DefaultProcessingViewer<MultiTsDocument2> {

    // FACTORY METHODS >
    public static Ts2ProcessingViewer2 create(MultiTsDocument2 doc, String y, String z) {
        Ts2ProcessingViewer2 viewer = new Ts2ProcessingViewer2(Type.APPLY, y, z);
        if (doc != null) {
            viewer.setDocument(doc);
        }
        return viewer;
    }
    // < FACTORY METHODS
    // CONSTANTS
    private static final Font DROP_DATA_FONT = new JLabel().getFont().deriveFont(Font.ITALIC);
    // visual components
    private final JLabel dropDataLabely, dropDataLabelz;
    private final JLabel tsLabely, tsLabelz;
    private final JLabel specLabel;

    public Ts2ProcessingViewer2(Type type, String y, String z) {
        super(type);
        this.dropDataLabely = new JLabel("Drop " + y + " here");
        this.dropDataLabelz = new JLabel("Drop " + z + " here");
        dropDataLabely.setFont(DROP_DATA_FONT);
        dropDataLabelz.setFont(DROP_DATA_FONT);
        this.tsLabely = new JLabel(y);
        tsLabely.setVisible(false);
        this.tsLabelz = new JLabel(z);
        tsLabelz.setVisible(false);
        this.specLabel = new JLabel("Spec: ");
        specLabel.setVisible(false);

        toolBar.add(Box.createHorizontalStrut(3), 0);
        toolBar.add(dropDataLabely, 1);
        toolBar.add(tsLabely, 2);
        toolBar.add(new JToolBar.Separator(), 3);
        toolBar.add(dropDataLabelz, 4);
        toolBar.add(tsLabelz, 5);
        toolBar.add(new JToolBar.Separator(), 6);
        toolBar.add(specLabel, 7);

        TsHandler hy = new TsHandler(0);
        TsHandler hz = new TsHandler(1);
        tsLabely.setTransferHandler(hy);
        dropDataLabely.setTransferHandler(hy);
        tsLabelz.setTransferHandler(hz);
        dropDataLabelz.setTransferHandler(hz);
    }

    @Override
    public void refreshHeader() {
        MultiTsDocument2 doc = getDocument();
        if (doc == null) {
            return;
        }
        Ts[] input = (Ts[]) doc.getInput();
        if (input == null || input[0] == null) {
            dropDataLabely.setVisible(true);
            tsLabely.setVisible(false);
        } else {
            dropDataLabely.setVisible(false);
            TsMoniker monikery = input[0].getMoniker();
            tsLabely.setIcon(MonikerUI.getDefault().getIcon(monikery));
            tsLabely.setToolTipText(tsLabely.getText() + (monikery.getSource() != null ? (" (" + monikery.getSource() + ")") : ""));
            tsLabely.setVisible(true);
        }
        if (input == null || input[1] == null) {
            dropDataLabelz.setVisible(true);
            tsLabelz.setVisible(false);
        } else {
            dropDataLabelz.setVisible(false);
            TsMoniker monikerz = input[1].getMoniker();
            tsLabelz.setIcon(MonikerUI.getDefault().getIcon(monikerz));
            tsLabelz.setToolTipText(tsLabelz.getText() + (monikerz.getSource() != null ? (" (" + monikerz.getSource() + ")") : ""));
            tsLabelz.setVisible(true);
        }
        if (input != null) {
            IProcSpecification spec = doc.getSpecification();
            specLabel.setText("Spec: " + (spec != null ? spec.toString() : ""));
            specLabel.setVisible(true);
        } else {
            specLabel.setVisible(false);
        }
        this.toolBar.doLayout();

    }

    class TsHandler extends TransferHandler {

        private final int pos;

        TsHandler(int pos) {
            this.pos = pos;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return TssTransferSupport.getDefault().canImport(support.getDataFlavors());
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            Ts ts = TssTransferSupport.getDefault().toTs(support.getTransferable());
            if (ts != null) {
                Ts[] input = (Ts[]) getDocument().getInput();
                if (input == null) {
                    input = new Ts[2];
                }else
                    input=input.clone();
                input[pos] = ts;
                getDocument().setInput(input);
                refreshAll();
                return true;
            }
            return false;
        }
    }
}
