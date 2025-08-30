// Test file khusus untuk masalah rasio karat dan perhitungan perubahan harga
// Jalankan di browser console setelah halaman harga-emas.html dimuat

console.log('=== TESTING KARAT RATIO ISSUE ===');

// Test 1: Cek rasio karat saat ini
function checkCurrentKaratRatio() {
    if (typeof goldPriceManager !== 'undefined') {
        console.log('📊 Current karat ratio check:');
        const currentPrices = goldPriceManager.currentPrices;
        
        if (currentPrices['24k'] > 0) {
            const basePrice24k = currentPrices['24k'];
            const actual22k = currentPrices['22k'];
            const actual18k = currentPrices['18k'];
            
            // Calculate expected ratios
            const expected22k = Math.round(basePrice24k * 0.9167);
            const expected18k = Math.round(basePrice24k * 0.75);
            
            console.log(`  24K (base): ${basePrice24k.toLocaleString('id-ID')}`);
            console.log(`  22K actual: ${actual22k.toLocaleString('id-ID')}`);
            console.log(`  22K expected: ${expected22k.toLocaleString('id-ID')} (${basePrice24k} * 0.9167)`);
            console.log(`  22K difference: ${(actual22k - expected22k).toLocaleString('id-ID')}`);
            console.log(`  18K actual: ${actual18k.toLocaleString('id-ID')}`);
            console.log(`  18K expected: ${expected18k.toLocaleString('id-ID')} (${basePrice24k} * 0.75)`);
            console.log(`  18K difference: ${(actual18k - expected18k).toLocaleString('id-ID')}`);
            
            // Check if ratios are within tolerance
            const tolerance = 1000; // 1000 rupiah
            const ratio22kCorrect = Math.abs(actual22k - expected22k) <= tolerance;
            const ratio18kCorrect = Math.abs(actual18k - expected18k) <= tolerance;
            
            console.log(`  22K ratio correct: ${ratio22kCorrect ? '✅' : '❌'}`);
            console.log(`  18K ratio correct: ${ratio18kCorrect ? '✅' : '❌'}`);
            
            return { ratio22kCorrect, ratio18kCorrect };
        } else {
            console.log('❌ No 24K price available');
            return null;
        }
    } else {
        console.log('❌ GoldPriceManager tidak tersedia');
        return null;
    }
}

// Test 2: Test rasio karat dengan harga yang berbeda
function testKaratRatioWithDifferentPrices() {
    console.log('🧪 Testing karat ratio with different prices:');
    
    const testPrices = [1000000, 1500000, 2000000, 2500000, 3000000];
    
    testPrices.forEach(price24k => {
        const expected22k = Math.round(price24k * 0.9167);
        const expected18k = Math.round(price24k * 0.75);
        
        console.log(`  Base 24K: ${price24k.toLocaleString('id-ID')}`);
        console.log(`    Expected 22K: ${expected22k.toLocaleString('id-ID')} (${((expected22k/price24k)*100).toFixed(2)}%)`);
        console.log(`    Expected 18K: ${expected18k.toLocaleString('id-ID')} (${((expected18k/price24k)*100).toFixed(2)}%)`);
        console.log('    ---');
    });
}

// Test 3: Test perubahan harga dengan rasio yang benar
function testPriceChangeWithCorrectRatio() {
    console.log('🔄 Testing price change with correct ratio:');
    
    // Simulate manual update to 2 million
    const manualPrice24k = 2000000;
    const manualPrice22k = Math.round(manualPrice24k * 0.9167);
    const manualPrice18k = Math.round(manualPrice24k * 0.75);
    
    console.log('Manual update prices:');
    console.log(`  24K: ${manualPrice24k.toLocaleString('id-ID')}`);
    console.log(`  22K: ${manualPrice22k.toLocaleString('id-ID')}`);
    console.log(`  18K: ${manualPrice18k.toLocaleString('id-ID')}`);
    
    // Simulate external API update to 2.1 million (10% increase)
    const externalPrice24k = 2100000;
    const externalPrice22k = Math.round(externalPrice24k * 0.9167);
    const externalPrice18k = Math.round(externalPrice24k * 0.75);
    
    console.log('External API update prices:');
    console.log(`  24K: ${externalPrice24k.toLocaleString('id-ID')}`);
    console.log(`  22K: ${externalPrice22k.toLocaleString('id-ID')}`);
    console.log(`  18K: ${externalPrice18k.toLocaleString('id-ID')}`);
    
    // Calculate changes
    const change24k = externalPrice24k - manualPrice24k;
    const change22k = externalPrice22k - manualPrice22k;
    const change18k = externalPrice18k - manualPrice18k;
    
    const changePercent24k = ((change24k / manualPrice24k) * 100);
    const changePercent22k = ((change22k / manualPrice22k) * 100);
    const changePercent18k = ((change18k / manualPrice18k) * 100);
    
    console.log('Price changes:');
    console.log(`  24K: ${change24k.toLocaleString('id-ID')} (${changePercent24k.toFixed(2)}%)`);
    console.log(`  22K: ${change22k.toLocaleString('id-ID')} (${changePercent22k.toFixed(2)}%)`);
    console.log(`  18K: ${change18k.toLocaleString('id-ID')} (${changePercent18k.toFixed(2)}%)`);
    
    // Verify that all changes are approximately the same percentage
    const avgChangePercent = (changePercent24k + changePercent22k + changePercent18k) / 3;
    const tolerance = 0.1; // 0.1% tolerance
    
    const allChangesSimilar = Math.abs(changePercent24k - avgChangePercent) <= tolerance &&
                             Math.abs(changePercent22k - avgChangePercent) <= tolerance &&
                             Math.abs(changePercent18k - avgChangePercent) <= tolerance;
    
    console.log(`All changes similar (within ${tolerance}%): ${allChangesSimilar ? '✅' : '❌'}`);
    
    return { allChangesSimilar, avgChangePercent };
}

// Test 4: Test endpoint changes untuk verifikasi rasio
async function testChangesEndpointForRatio() {
    try {
        console.log('🔍 Testing changes endpoint for ratio verification...');
        const response = await fetch('/gold-price/changes/latest');
        const result = await response.json();
        
        if (response.ok && result.success && result.data) {
            console.log('✅ Changes endpoint berfungsi');
            console.log('Total changes:', result.data.length);
            
            // Group changes by source and purity
            const changesBySource = {};
            result.data.forEach(change => {
                if (!changesBySource[change.changeSource]) {
                    changesBySource[change.changeSource] = {};
                }
                if (!changesBySource[change.changeSource][change.purity]) {
                    changesBySource[change.changeSource][change.purity] = [];
                }
                changesBySource[change.changeSource][change.purity].push(change);
            });
            
            console.log('📊 Changes grouped by source and purity:');
            Object.keys(changesBySource).forEach(source => {
                console.log(`  ${source}:`);
                Object.keys(changesBySource[source]).forEach(purity => {
                    const changes = changesBySource[source][purity];
                    console.log(`    ${purity}: ${changes.length} changes`);
                    changes.forEach((change, index) => {
                        console.log(`      ${index + 1}. ${change.oldPrice.toLocaleString('id-ID')} -> ${change.newPrice.toLocaleString('id-ID')} (${change.changeType}, ${change.changePercent.toFixed(2)}%)`);
                    });
                });
            });
            
            // Check for ratio consistency
            Object.keys(changesBySource).forEach(source => {
                if (changesBySource[source]['24k'] && changesBySource[source]['22k'] && changesBySource[source]['18k']) {
                    console.log(`\n🔍 Ratio consistency check for ${source}:`);
                    
                    const latest24k = changesBySource[source]['24k'][0];
                    const latest22k = changesBySource[source]['22k'][0];
                    const latest18k = changesBySource[source]['18k'][0];
                    
                    if (latest24k && latest22k && latest18k) {
                        const expected22k = Math.round(latest24k.newPrice * 0.9167);
                        const expected18k = Math.round(latest24k.newPrice * 0.75);
                        
                        const ratio22kCorrect = Math.abs(latest22k.newPrice - expected22k) <= 1000;
                        const ratio18kCorrect = Math.abs(latest18k.newPrice - expected18k) <= 1000;
                        
                        console.log(`  24K: ${latest24k.newPrice.toLocaleString('id-ID')}`);
                        console.log(`  22K actual: ${latest22k.newPrice.toLocaleString('id-ID')}, expected: ${expected22k.toLocaleString('id-ID')} (${ratio22kCorrect ? '✅' : '❌'})`);
                        console.log(`  18K actual: ${latest18k.newPrice.toLocaleString('id-ID')}, expected: ${expected18k.toLocaleString('id-ID')} (${ratio18kCorrect ? '✅' : '❌'})`);
                    }
                }
            });
            
            return changesBySource;
        } else {
            console.log('❌ Changes endpoint error:', response.status);
            return null;
        }
    } catch (error) {
        console.error('❌ Error testing changes endpoint:', error);
        return null;
    }
}

// Test 5: Simulate manual update dan external update untuk test rasio
async function simulateUpdateSequenceForRatio() {
    try {
        console.log('🧪 Simulating update sequence for ratio test...');
        if (typeof goldPriceManager !== 'undefined') {
            const currentPrices = { ...goldPriceManager.currentPrices };
            console.log('Current prices before simulation:', currentPrices);
            
            // Step 1: Simulate manual update to 2 million
            const manualPrice24k = 2000000;
            console.log(`\nStep 1: Simulating manual update to 24K: ${manualPrice24k.toLocaleString('id-ID')}`);
            
            const manualResponse = await fetch('/gold-price/update/manual', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    hargaJual24k: manualPrice24k,
                    hargaJual22k: Math.round(manualPrice24k * 0.9167),
                    hargaJual18k: Math.round(manualPrice24k * 0.75)
                })
            });
            
            if (manualResponse.ok) {
                const manualResult = await manualResponse.json();
                console.log('Manual update result:', manualResult);
                
                // Wait for backend to process
                await new Promise(resolve => setTimeout(resolve, 3000));
                
                // Step 2: Simulate external API update to 2.1 million
                const externalPrice24k = 2100000;
                console.log(`\nStep 2: Simulating external API update to 24K: ${externalPrice24k.toLocaleString('id-ID')}`);
                
                const externalResponse = await fetch('/gold-price/update', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ harga24k: externalPrice24k })
                });
                
                if (externalResponse.ok) {
                    const externalResult = await externalResponse.json();
                    console.log('External update result:', externalResult);
                    
                    // Wait for backend to process
                    await new Promise(resolve => setTimeout(resolve, 3000));
                    
                    // Check final state
                    console.log('\nFinal state check:');
                    await goldPriceManager.reliableRefreshData();
                    
                    const finalPrices = goldPriceManager.currentPrices;
                    const finalChanges = goldPriceManager.priceChanges;
                    
                    console.log('Final prices:', finalPrices);
                    console.log('Final changes:', finalChanges);
                    
                    // Verify ratios
                    if (finalPrices['24k'] > 0) {
                        const expected22k = Math.round(finalPrices['24k'] * 0.9167);
                        const expected18k = Math.round(finalPrices['24k'] * 0.75);
                        
                        const ratio22kCorrect = Math.abs(finalPrices['22k'] - expected22k) <= 1000;
                        const ratio18kCorrect = Math.abs(finalPrices['18k'] - expected18k) <= 1000;
                        
                        console.log('Final ratio verification:');
                        console.log(`  24K: ${finalPrices['24k'].toLocaleString('id-ID')}`);
                        console.log(`  22K: ${finalPrices['22k'].toLocaleString('id-ID')} (expected: ${expected22k.toLocaleString('id-ID')}) - ${ratio22kCorrect ? '✅' : '❌'}`);
                        console.log(`  18K: ${finalPrices['18k'].toLocaleString('id-ID')} (expected: ${expected18k.toLocaleString('id-ID')}) - ${ratio18kCorrect ? '✅' : '❌'}`);
                    }
                    
                    return true;
                } else {
                    console.log('❌ External update failed:', externalResponse.status);
                    return false;
                }
            } else {
                console.log('❌ Manual update failed:', manualResponse.status);
                return false;
            }
        } else {
            console.log('❌ GoldPriceManager tidak tersedia');
            return false;
        }
    } catch (error) {
        console.error('❌ Error simulating update sequence:', error);
        return false;
    }
}

// Jalankan semua test
async function runAllKaratRatioTests() {
    console.log('🚀 Running all karat ratio tests...');
    
    checkCurrentKaratRatio();
    testKaratRatioWithDifferentPrices();
    testPriceChangeWithCorrectRatio();
    await testChangesEndpointForRatio();
    await simulateUpdateSequenceForRatio();
    
    console.log('✅ All karat ratio tests completed');
}

// Export functions untuk testing manual
window.testKaratRatio = {
    checkCurrentKaratRatio,
    testKaratRatioWithDifferentPrices,
    testPriceChangeWithCorrectRatio,
    testChangesEndpointForRatio,
    simulateUpdateSequenceForRatio,
    runAllKaratRatioTests
};

console.log('Karat ratio test functions available at: window.testKaratRatio');
console.log('Run: window.testKaratRatio.runAllKaratRatioTests() to test everything');
console.log('Or run individual tests like: window.testKaratRatio.checkCurrentKaratRatio()');