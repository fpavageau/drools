/*
* Copyright 2012 Red Hat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.drools.compiler.beliefsystem.defeasible;

import org.drools.core.BeliefSystemType;
import org.drools.core.ClassObjectFilter;
import org.drools.core.SessionConfiguration;
import org.drools.core.beliefsystem.BeliefSet;
import org.drools.core.beliefsystem.defeasible.DefeasibilityStatus;
import org.drools.core.beliefsystem.defeasible.DefeasibleBeliefSet;
import org.drools.core.common.EqualityKey;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.NamedEntryPoint;
import org.drools.core.common.TruthMaintenanceSystem;
import org.drools.core.util.Iterator;
import org.drools.core.util.ObjectHashMap;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.definition.type.FactType;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefeasibilityTest {

    protected StatefulKnowledgeSession getSessionFromString( String drlString) {
        KnowledgeBuilder kBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kBuilder.add( ResourceFactory.newByteArrayResource(drlString.getBytes()),
                      ResourceType.DRL );
        if ( kBuilder.hasErrors() ) {
            System.err.println( kBuilder.getErrors() );
            fail();
        }

        KnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase( );
        kBase.addKnowledgePackages( kBuilder.getKnowledgePackages() );

        KieSessionConfiguration ksConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        ((SessionConfiguration) ksConf).setBeliefSystemType( BeliefSystemType.DEFEASIBLE );

        StatefulKnowledgeSession kSession = kBase.newStatefulKnowledgeSession( ksConf, null );
        return kSession;
    }


    protected StatefulKnowledgeSession getSession( String ruleFile ) {
        KnowledgeBuilder kBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kBuilder.add( ResourceFactory.newClassPathResource( ruleFile ),
                ResourceType.DRL );
        if ( kBuilder.hasErrors() ) {
            System.err.println( kBuilder.getErrors() );
            fail();
        }

        KnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase();
        kBase.addKnowledgePackages( kBuilder.getKnowledgePackages() );


        KieSessionConfiguration ksConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        ((SessionConfiguration) ksConf).setBeliefSystemType( BeliefSystemType.DEFEASIBLE );

        StatefulKnowledgeSession kSession = kBase.newStatefulKnowledgeSession( ksConf, null );
        return kSession;
    }


    private void checkStatus( EqualityKey key, int support, DefeasibilityStatus status ) {
        assertEquals( EqualityKey.JUSTIFIED, key.getStatus() );
        BeliefSet set = key.getBeliefSet();
        assertTrue( set instanceof DefeasibleBeliefSet );
        DefeasibleBeliefSet dfs = ( DefeasibleBeliefSet ) set;
//        LinkedListNode n = dfs.getFirst();
//        do {
//            System.out.println( n );
//            n = (LinkedListNode) n.getNext();
//        } while ( n != null );

        assertEquals( support, dfs.size() );
        assertEquals( status, dfs.getStatus() );
    }



    @Test
    public void testStrictEntailment() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/strict.drl" );
        kSession.fireAllRules();

//        TruthMaintenanceSystem tms = ( (StatefulKnowledgeSessionImpl) kSession ).session.getTruthMaintenanceSystem();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Ctype = kSession.getKieBase().getFactType( "org.drools.defeasible", "C" );
        FactType Dtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "D" );


        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Ctype.getFactClass() ) {
                checkStatus( key, 2, DefeasibilityStatus.DEFINITELY );
            } else if ( factClass == Dtype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        assertEquals( 5, kSession.getObjects().size() );
    }



    @Test
    public void testDefeasibleEntailmentWithStrictOverride() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/strictOverride.drl" );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Ctype = kSession.getKieBase().getFactType( "org.drools.defeasible", "C" );
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );


        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Ctype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        for ( Object o : kSession.getObjects() ) {
            System.out.println( o );
        }
        assertEquals( 5, kSession.getObjects().size() );

    }



    @Test
    public void defeasibleEntailmentMultiActivation() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/defeat.drl" );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );


        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 3, DefeasibilityStatus.UNDECIDABLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        for ( Object o : kSession.getObjects() ) {
            System.out.println( o );
        }
        assertEquals( 3, kSession.getObjects().size() );
    }





    @Test
    public void testDefeasibleEntailmentMultiActivationWithDefeatComposite() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/defeatDefeater.drl" );
        ArrayList list = new ArrayList();
        kSession.setGlobal( "list", list );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );


        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFEATEDLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }


        for ( Object o : kSession.getObjects() ) {
            System.out.println( o );
        }
        assertEquals( 4, kSession.getObjects().size() );
        assertEquals( 1, list.size() );
        assertTrue( list.contains( "Stage1" ) );



        kSession.insert( "go" );
        kSession.fireAllRules();

        keys = tms.getEqualityKeyMap();
        iter = keys.iterator();
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 3, DefeasibilityStatus.DEFEASIBLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        assertEquals( 3, list.size() );
        assertTrue( list.contains( "Stage1" ) );
        assertTrue( list.contains( "Stage2" ) );

    }


    @Test
    public void testDefeaterNeutrality() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/defeaterOnly.drl" );
        ArrayList list = new ArrayList();
        kSession.setGlobal( "list", list );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );

        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFEATEDLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        for ( Object o : kSession.getObjects() ) {
            System.out.println( o );
        }
        assertEquals( 0, list.size() );
        assertEquals( 1, kSession.getFactCount() );
    }


    @Test
    public void testMultipleDefeats() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/multiDefeat.drl" );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );


        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 2, DefeasibilityStatus.DEFEATEDLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        for ( Object o : kSession.getObjects() ) {
            System.out.println( o );
        }
        assertEquals( 2, kSession.getObjects().size() );


        kSession.fireAllRules();
    }


    @Test
    public void testRemoveDefiniteJustifier() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/strictRetract.drl" );

        FactHandle h = kSession.insert( "go" );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Ctype = kSession.getKieBase().getFactType( "org.drools.defeasible", "C" );
        FactType Atype = kSession.getKieBase().getFactType( "org.drools.defeasible", "A" );

        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Ctype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else if ( factClass == Atype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        kSession.retract( h );
        kSession.fireAllRules();

        keys = tms.getEqualityKeyMap();
        iter = keys.iterator();
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Ctype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFEASIBLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

    }

    @Test
    public void testRemoveDefeasibleJustifier() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/defeaterRetract.drl" );

        FactHandle h = kSession.insert( "go" );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Ctype = kSession.getKieBase().getFactType( "org.drools.defeasible", "C" );
        FactType Atype = kSession.getKieBase().getFactType( "org.drools.defeasible", "A" );

        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Ctype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else if ( factClass == Atype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        kSession.retract( h );
        kSession.fireAllRules();

        keys = tms.getEqualityKeyMap();
        iter = keys.iterator();
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Ctype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFINITELY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

    }



    @Test
    public void testRemoveDefeasibleEntailmentMultiActivationWithDefeat() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/defeatDefeaterRetract.drl" );
        ArrayList list = new ArrayList();

        kSession.setGlobal( "list", list );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );

        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFEATEDLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        assertEquals( 1, list.size() );
        assertTrue( list.contains( "Stage1" ) );
        assertEquals( 4, kSession.getObjects().size() );

        FactHandle h = kSession.insert( "go" );
        kSession.fireAllRules();

        keys = tms.getEqualityKeyMap();
        iter = keys.iterator();
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 3, DefeasibilityStatus.DEFEASIBLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        kSession.retract( h );
        kSession.fireAllRules();

        keys = tms.getEqualityKeyMap();
        iter = keys.iterator();
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFEATEDLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }


//        checkDefeasibilityByHandleId(4, DEFEASIBILITY_STATUS.DEFEATEDLY, 3, 1, tms);



    }



    @Test
    public void testDefeaterPositiveVsNegative() {
        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/defeatersPosNeg.drl" );
        ArrayList list = new ArrayList();
        kSession.setGlobal( "list", list );
        kSession.fireAllRules();


        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );

        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Object fact = key.getFactHandle().getObject();
            Class factClass = fact.getClass();
            if ( factClass == Xtype.getFactClass() ) {
                Integer val = (Integer) Xtype.get( fact, "id" );
                switch ( val ) {
                    case -1 :
                        checkStatus( key, 2, DefeasibilityStatus.UNDECIDABLY );
                        break;
                    case 3 :
                        checkStatus( key, 1, DefeasibilityStatus.DEFEATEDLY );
                        break;
                    case -35 :
                        checkStatus( key, 3, DefeasibilityStatus.UNDECIDABLY );
                        break;
                    case 44 :
                        checkStatus( key, 2, DefeasibilityStatus.DEFEASIBLY );
                        break;
                    default : fail( "Unrecognized fact" );
                }
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        for ( Object o : kSession.getObjects() ) {
            System.out.println( o );
        }
        System.err.println( list );

        assertEquals( 1, list.size() );
        assertTrue( list.contains( -44 ) );
        assertTrue( ! list.contains( -35 ) );
        assertEquals( 4, kSession.getFactCount() );
    }





    @Test
    public void testDefeatOutcomePosNeg() {

        StatefulKnowledgeSession kSession = getSession( "org/drools/compiler/beliefsystem/defeasible/negDefeatPos.drl" );
        ArrayList list = new ArrayList();
        kSession.setGlobal( "list", list );
        kSession.fireAllRules();

        TruthMaintenanceSystem tms = ((NamedEntryPoint) kSession.getEntryPoint( "DEFAULT" )).getTruthMaintenanceSystem();
        FactType Xtype = kSession.getKieBase().getFactType( "org.drools.defeasible", "X" );

        ObjectHashMap keys = tms.getEqualityKeyMap();
        Iterator iter = keys.iterator();
        ObjectHashMap.ObjectEntry entry;
        while ( ( entry = ( ObjectHashMap.ObjectEntry) iter.next() ) != null ) {
            EqualityKey key = (EqualityKey) entry.getValue();

            Class factClass = key.getFactHandle().getObject().getClass();
            if ( factClass == Xtype.getFactClass() ) {
                checkStatus( key, 1, DefeasibilityStatus.DEFEASIBLY );
            } else {
                fail( "Unrecognized object has been logically justified : " + factClass );
            }
        }

        for ( Object o : kSession.getObjects() ) {
            System.out.println( o );
        }
        assertEquals( 2, kSession.getObjects().size() );
        assertEquals( 1, list.size() );
        assertTrue( list.contains( "-1" ) );

    }


    @Test
    public void testPrimeJustificationWithEqualityMode() {
        String droolsSource =
                "package org.drools.tms.test; \n" +
                "declare Bar end \n" +
                "" +
                "declare Holder x : Bar end \n" +
                "" +
                "" +
                "rule Init \n" +
                "when \n" +
                "then \n" +
                "   insert( new Holder( new Bar() ) ); \n" +
                "end \n" +

                "rule Justify \n" +
                "when \n" +
                " $s : Integer() \n" +
                " $h : Holder( $b : x ) \n" +
                "then \n" +
                " insertLogical( $b ); \n" +
                "end \n" +

                "rule React \n" +
                "when \n" +
                " $b : Bar(  ) \n" +
                "then \n" +
                " System.out.println( $b );  \n" +
                "end \n" ;

        /////////////////////////////////////

        StatefulKnowledgeSession session = getSessionFromString( droolsSource );

        FactHandle handle1 = session.insert( 10 );
        FactHandle handle2 = session.insert( 20 );

        assertEquals( 4, session.fireAllRules() );

        session.delete( handle1 );
        assertEquals( 0, session.fireAllRules() );
    }



    @Test
    public void testWMStatusOnNegativeDefeat() {
        String droolsSource =
                "package org.drools.tms.test; " +
                "global java.util.List posList;" +
                "global java.util.List negList;" +

                "declare Bar value : int @key end " +

                "declare entry-point 'neg' end " +

                "rule Top " +
                "@Defeasible " +
                "@Defeats( 'Sub' ) " +
                "when " +
                "   $i : Integer( this < 10 ) " +
                "then " +
                "   insertLogical( new Bar( $i ) ); " +
                "end " +

                "rule Sub " +
                "@Defeasible " +
                "when " +
                "   $i : Integer() " +
                "then " +
                "   insertLogical( new Bar( $i ), $i > 10 ? 'pos' : 'neg' ); " +
                "end " +

                "rule Sup " +
                "@Defeasible " +
                "@Defeats( 'Sub' ) " +
                "when " +
                "   $i : Integer( this > 10 ) " +
                "then " +
                "   insertLogical( new Bar( $i ), 'neg' ); " +
                "end " +

                "rule React_Pos " +
                "when " +
                "   $b : Bar() " +
                "then " +
                "   posList.add( $b ); " +
                "   System.out.println( ' ++++ ' + $b ); " +
                "end " +

                "rule React_Neg " +
                "when " +
                "   $b : Bar() from entry-point 'neg' " +
                "then " +
                "   negList.add( $b ); " +
                "   System.out.println( ' ---- ' + $b ); " +
                "end " +

                "";

        /////////////////////////////////////

        StatefulKnowledgeSession session = getSessionFromString( droolsSource );
        List posList = new ArrayList();
        List negList = new ArrayList();
        session.setGlobal( "posList", posList );
        session.setGlobal( "negList", negList );

        session.insert( 20 );
        session.insert( 5 );

        session.fireAllRules();

        assertEquals( 1, posList.size() );
        assertEquals( 1, negList.size() );

        Object posBar = posList.get( 0 );
        InternalFactHandle posHandle = (InternalFactHandle) session.getFactHandle( posBar );
        DefeasibleBeliefSet dbs = (DefeasibleBeliefSet) posHandle.getEqualityKey().getBeliefSet();
        assertEquals( 1, dbs.size() );
        assertFalse( dbs.isNegated() );
        assertFalse( dbs.isUndecided() );
        assertTrue( dbs.isPositive() );
        assertSame( posHandle, dbs.getPositiveFactHandle() );
        assertNull( dbs.getNegativeFactHandle() );
        assertTrue(  dbs.isDefeasiblyPosProveable() );
        assertTrue( session.getObjects().contains( posBar ) );

        Object negBar = negList.get( 0 );
        InternalFactHandle negHandle = (InternalFactHandle) session.getEntryPoint( "neg" ).getFactHandle( negBar );
        dbs = (DefeasibleBeliefSet) negHandle.getEqualityKey().getBeliefSet();
        assertEquals( 1, dbs.size() );
        assertFalse( dbs.isPositive() );
        assertFalse( dbs.isUndecided() );
        assertTrue( dbs.isNegated() );
        assertSame( negHandle, dbs.getNegativeFactHandle() );
        assertNull( dbs.getPositiveFactHandle() );
        assertTrue(  dbs.isDefeasiblyNegProveable() );
        assertFalse( session.getObjects().contains( negBar ) );

    }




    @Test( timeout = 10000)
    @Ignore
    public void testSelfDefeatWithRebuttal() {
        String droolsSource =
                "package org.drools.tms.test; " +
                "global java.util.List posList;" +
                "global java.util.List negList;" +

                "declare Bar value : int @key end " +

                "declare entry-point 'neg' end " +

                "query bar( Integer $i, Bar $b ) " +
                "   $b := Bar( $i ; ) " +
                "end " +

                "rule Create " +
                "@Defeasible " +
                "when " +
                "   $i : Integer() " +
                "then " +
                "   System.out.println( 'Create Bar ' + $i ); " +
                "   insertLogical( new Bar( $i ) ); " +
                "end " +

                "rule Defeater " +
                "@Direct " +
                "@Defeasible " +
                "@Defeats( 'Create' ) " +
                "when " +
                "   $b1 : Bar( $val1 : value ) " +
                "   $b2 : Bar( $val2 : value > $val1, value - $val1 < 15 ) " +
                "then " +
                "   System.out.println( $b2 + ' defeats ' + $b1 ); " +
                "   insertLogical( new Bar( $val1 ), 'neg' ); " +
                "end " +

                "rule ReactP " +
                "when " +
                "   $b : Bar() " +
                "then " +
                "   posList.add( $b ); " +
                "   System.out.println( ' ++++ ' + $b ); " +
                "end " +

                "rule ReactN " +
                "when " +
                "   $b : Bar() from entry-point 'neg' " +
                "then " +
                "   negList.add( $b ); " +
                "   System.out.println( ' ---- ' + $b ); " +
                "end " ;

        /////////////////////////////////////

        StatefulKnowledgeSession session = getSessionFromString( droolsSource );
        List posList = new ArrayList();
        List negList = new ArrayList();
        session.setGlobal( "posList", posList );
        session.setGlobal( "negList", negList );

        session.insert( 10 );
        session.insert( 20 );
        session.insert( 30 );

        session.fireAllRules();

        assertEquals( 2, posList.size() );
        assertEquals( 1, negList.size() );

    }


    @Test
    public void testDefeatersAndDefeasibles() {
        String droolsSource =
                "package org.drools.tms.test; " +
                "global java.util.List posList;" +

                "declare Bar value : int @key end " +

                "rule B " +
                "@Defeater " +
                "@Defeats( 'C' ) " +
                "when " +
                "   $i : Integer() " +
                "then " +
                "   insertLogical( new Bar( $i ) ); " +
                "end " +

                "rule C " +
                "@Defeasible " +
                "when " +
                "   $i : Integer() " +
                "then " +
                "   insertLogical( new Bar( $i ) ); " +
                "end " +

                "rule React " +
                "when " +
                "   $b : Bar() " +
                "then " +
                "   posList.add( $b ); " +
                "   System.out.println( ' ++++ ' + $b ); " +
                "end " ;

        /////////////////////////////////////

        StatefulKnowledgeSession session = getSessionFromString( droolsSource );
        List posList = new ArrayList();
        session.setGlobal( "posList", posList );

        session.insert( 10 );

        session.fireAllRules();
        assertEquals( 1, posList.size() );
    }


    @Test
    public void testManyDefeasibles() {
        String drl = "package org.drools.defeasible; " +
                     "declare Fact " +
                     "     fact: String @key " +
                     "end " +
                     " " +
                     "rule init " +
                     "     when " +
                     "     then " +
                     "         insert( new Fact( 'one' ) ); " +
                     "         insert( new Fact( 'two' ) ); " +
                     "         insert( new Fact( 'two' ) ); " +
                     "end " +
                     " " +
                     "rule rule1 " +
                     "     @Defeasible " +
                     "     enabled true " +
                     "     when " +
                     "         Fact( \"one\"; ) " +
                     "     then " +
                     "         System.out.println(\"one causes wibble\"); " +
                     "         insertLogical( new Fact( \"wibble\") ); " +
                     "end " +
                     " " +
                     "rule rule2 " +
                     "     @Defeasible " +
                     "     when " +
                     "         Fact( \"two\"; ) " +
                     "     then " +
                     "         System.out.println(\"two causes wibble\"); " +
                     "         insertLogical( new Fact( \"wibble\") ); " +
                     "end " +
                     " " +
                     "rule rule3 " +
                     "     @Defeater " +
                     "     @Defeats( \"rule2\" ) " +
                     "     when " +
                     "         Fact( \"two\"; ) " +
                     "     then " +
                     "         System.out.println(\"two negates wibble\"); " +
                     "         insertLogical( new Fact( \"wibble\"), \"neg\" ); " +
                     "end";

        StatefulKnowledgeSession session = getSessionFromString( drl );
        session.fireAllRules();

        FactType factType = session.getKieBase().getFactType( "org.drools.defeasible", "Fact" );
        for ( Object o : session.getObjects( new ClassObjectFilter( factType.getFactClass() ) ) ) {
            if ( "wibble".equals( factType.get( o, "fact" ) ) ) {
                InternalFactHandle handle = (InternalFactHandle) session.getFactHandle( o );
                DefeasibleBeliefSet dbs = (DefeasibleBeliefSet) handle.getEqualityKey().getBeliefSet();

                assertEquals( 3, dbs.size() );
                assertTrue( dbs.isConflicting() );
            }
        }

    }

}
