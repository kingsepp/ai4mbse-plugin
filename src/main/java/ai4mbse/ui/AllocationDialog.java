package ai4mbse.ui;
import ai4mbse.model.AllocationCandidate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog zur Anzeige und Auswahl von KI-generierten Allokationsempfehlungen.
 * 
 * Dieser Dialog zeigt eine Tabelle mit allen von der KI vorgeschlagenen Subsystem-Allokationen
 * für ein ausgewähltes Requirement an. Der Benutzer kann die gewünschten Allokationen 
 * auswählen, bevor sie als Satisfy-Dependencies im Cameo Systems Modeler erstellt werden.
 * 
 * Die Tabelle enthält folgende Spalten:
 * - Auswahlcheckbox
 * - Subsystemname
 * - Konfidenzwert (als Prozent)
 * - Begründung der KI
 * 
 * @author AI4MBSE Plugin
 * @version 1.0
 */
public class AllocationDialog extends JDialog {

    /** Liste der von der KI vorgeschlagenen Allokationskandidaten */
    private List<AllocationCandidate> candidates;
    
    /** Liste der vom Benutzer akzeptierten Allokationen */
    private List<AllocationCandidate> acceptedAllocations = new ArrayList<>();
    
    /** Haupttabelle zur Anzeige der Allokationskandidaten */
    private JTable table;
    
    /** Table Model für die Verwaltung der Tabellendaten */
    private DefaultTableModel tableModel;
    
    /** Referenz auf das Hauptfenster für die Positionierung des Dialogs */
    private Frame dialogOwner;


    /**
     * Konstruktor für den Allokationsdialog.
     * 
     * @param owner Das Hauptfenster als Parent für diesen Dialog
     * @param candidates Liste der Allokationskandidaten, die angezeigt werden sollen
     */
    public AllocationDialog(Frame owner, List<AllocationCandidate> candidates) {
        super(owner, "Confirm Subsystem Allocations", false); // NON-MODAL!
        this.candidates = candidates;
        this.dialogOwner = owner;
        initComponents();
    }

    /**
     * Initialisiert alle UI-Komponenten des Dialogs.
     * Erstellt die Tabelle, Buttons und das Layout.
     */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(700, 400));

        // Header mit Requirement-Name
        JLabel headerLabel = new JLabel("AI-Suggested Allocations for Requirement: " + 
            (candidates != null && !candidates.isEmpty() ? candidates.get(0).getRequirementElement().getHumanName() : "N/A"), SwingConstants.CENTER);
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(headerLabel, BorderLayout.NORTH);

        // Tabelle für Allokationskandidaten
        String[] columnNames = { "Select", "Subsystem Name", "Confidence", "Justification" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            /**
             * Definiert die Datentypen für jede Tabellenspalte.
             * 
             * @param columnIndex Index der Spalte
             * @return Class-Objekt des entsprechenden Datentyps
             */
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class; // Checkbox für Auswahl
                if (columnIndex == 2) return String.class;  // Konfidenz als formatierter String
                return String.class;
            }

            /**
             * Bestimmt, welche Zellen editierbar sind.
             * 
             * @param row Zeilenindex
             * @param column Spaltenindex
             * @return true, wenn die Zelle editierbar ist
             */
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Nur die Checkbox-Spalte ist editierbar
            }
        };

        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Spaltenbreiten festlegen
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);  // Auswahl-Checkbox
        columnModel.getColumn(1).setPreferredWidth(150); // Subsystemname
        columnModel.getColumn(2).setPreferredWidth(50);  // Konfidenz
        columnModel.getColumn(3).setPreferredWidth(300); // Begründung

        // Angenommen, 'table' ist deine JTable
        table.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JTextArea area = new javax.swing.JTextArea();
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setText(value != null ? value.toString() : "");
                area.setOpaque(true);
                area.setFont(table.getFont());

                if (isSelected) {
                    area.setBackground(table.getSelectionBackground());
                    area.setForeground(table.getSelectionForeground());
                } else {
                    area.setBackground(table.getBackground());
                    area.setForeground(table.getForeground());
                }

                // Dynamische Höhe: setSize MUSS vorher gesetzt werden!
                int colWidth = table.getColumnModel().getColumn(column).getWidth();
                area.setSize(new Dimension(colWidth, Short.MAX_VALUE));
                int preferredHeight = area.getPreferredSize().height;
                if (table.getRowHeight(row) != preferredHeight) {
                    table.setRowHeight(row, preferredHeight);
                }
                return area;
            }
        });



        populateTable();

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // --- Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Sammelt alle ausgewählten Allokationen
                acceptedAllocations.clear();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
                    if (isSelected != null && isSelected) {
                        acceptedAllocations.add(candidates.get(i));
                    }
                }
                
                // Listener benachrichtigen (nicht-blockierend)
                if (dialogListener != null) {
                    dialogListener.onAllocationsAccepted(new ArrayList<>(acceptedAllocations));
                }
                
                dispose();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptedAllocations.clear();
                
                // Listener über Abbruch benachrichtigen
                if (dialogListener != null) {
                    dialogListener.onDialogCancelled();
                }
                
                dispose();
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(dialogOwner); // Center on parent frame
    }

    /**
     * Füllt die Tabelle mit den Allokationskandidaten.
     * Sortiert die Kandidaten nach Konfidenzwert (höchster zuerst) und 
     * wählt automatisch Kandidaten mit hoher Konfidenz (>= 0.5) vor.
     */
    private void populateTable() {
        if (candidates == null) return;
        
        // Sortierung nach Konfidenzwert (höchster zuerst)
        candidates.sort((c1, c2) -> Double.compare(c2.getConfidence(), c1.getConfidence()));

        for (AllocationCandidate candidate : candidates) {
            // Automatische Vorauswahl bei hoher Konfidenz
            boolean preSelected = candidate.getConfidence() >= 0.5;
            candidate.setSelectedForAllocation(preSelected);

            tableModel.addRow(new Object[] {
                preSelected,
                candidate.getSubsystemName(),
                String.format("%.0f %%", candidate.getConfidence() * 100),
                candidate.getJustification()
            });
        }
    }

    /**
     * Gibt die Liste der vom Benutzer akzeptierten Allokationen zurück.
     * 
     * @return Liste der akzeptierten AllocationCandidate-Objekte
     */
    public List<AllocationCandidate> getAcceptedAllocations() {
        return acceptedAllocations;
    }
    
    /**
     * Callback-Interface für die Benachrichtigung über Dialog-Ergebnisse.
     */
    public interface AllocationDialogListener {
        /**
         * Wird aufgerufen, wenn der Benutzer Allokationen bestätigt hat.
         * 
         * @param acceptedAllocations Liste der akzeptierten Allokationen
         */
        void onAllocationsAccepted(List<AllocationCandidate> acceptedAllocations);
        
        /**
         * Wird aufgerufen, wenn der Dialog abgebrochen wurde.
         */
        void onDialogCancelled();
    }
    
    /** Listener für Dialog-Ergebnisse */
    private AllocationDialogListener dialogListener;
    
    /**
     * Setzt den Listener für Dialog-Ergebnisse.
     * 
     * @param listener Der Listener für Dialog-Events
     */
    public void setDialogListener(AllocationDialogListener listener) {
        this.dialogListener = listener;
    }
}